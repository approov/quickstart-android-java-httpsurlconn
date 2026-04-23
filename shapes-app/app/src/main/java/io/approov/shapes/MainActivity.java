//
// MIT License
//
// Copyright (c) 2016-present, Critical Blue Ltd.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
// (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
// publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
// THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package io.approov.shapes;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

// *** UNCOMMENT THE LINE BELOW FOR APPROOV ***
import io.approov.service.httpsurlconn.ApproovService;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MESSAGE_SIGNING_BATCH_SIZE = 100;
    private Activity activity;
    private View statusView = null;
    private ImageView statusImageView = null;
    private TextView statusTextView = null;
    private Button helloCheckButton = null;
    private Button shapesCheckButton = null;

    /**
     * Helper method to read the response from the Shapes endpoint and to select the
     * appropriate shapes image.
     *
     * @param connection provides the response from the endpoint
     * @return resource identifier to show
     */
    private int readShapesResponse(HttpsURLConnection connection) {
        // read the response String
        String result = null;
        StringBuilder sb = new StringBuilder();
        InputStream is = null;
        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {
            Log.d(TAG, "Error reading Shapes response InputStream");
            return R.drawable.confused;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.d(TAG, "Error closing Shapes response InputStream");
                }
            }
        }

        // decode the JSON to get the response
        JSONObject shapeJSON = null;
        try {
            shapeJSON = new JSONObject(result);
        } catch (JSONException e) {
            Log.d(TAG, "Invalid JSON from Shapes response");
            return R.drawable.confused;
        }
        if (shapeJSON != null) {
            try {
                String shape = shapeJSON.getString("shape");
                if (shape != null) {
                    if (shape.equalsIgnoreCase("square")) {
                        return R.drawable.square;
                    } else if (shape.equalsIgnoreCase("circle")) {
                        return R.drawable.circle;
                    } else if (shape.equalsIgnoreCase("rectangle")) {
                        return R.drawable.rectangle;
                    } else if (shape.equalsIgnoreCase("triangle")) {
                        return R.drawable.triangle;
                    }
                }
            } catch (JSONException e) {
                Log.d(TAG, "JSONException from Shapes response: " + e.toString());
            }
        }
        return R.drawable.confused;
    }

    private String readResponseBody(HttpsURLConnection connection) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = null;
        try {
            try {
                is = new BufferedInputStream(connection.getInputStream());
            } catch (IOException e) {
                InputStream errorStream = connection.getErrorStream();
                if (errorStream == null)
                    throw e;
                is = new BufferedInputStream(errorStream);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = br.readLine()) != null)
                sb.append(inputLine);
            return sb.toString();
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.d(TAG, "Error closing Shapes response InputStream");
                }
            }
        }
    }

    private static final class BatchResult {
        int validAccepted;
        int validRejected;
        int tamperedAccepted;
        int tamperedRejected;
        int networkErrors;
        int unexpectedResponses;
    }

    private String mutateSignatureHeader(String signatureHeader) {
        if (signatureHeader == null)
            return null;

        int firstColon = signatureHeader.indexOf(':');
        int lastColon = signatureHeader.lastIndexOf(':');
        if ((firstColon < 0) || (lastColon <= firstColon + 1))
            return signatureHeader + "A";

        char[] chars = signatureHeader.toCharArray();
        for (int i = firstColon + 1; i < lastColon; i++) {
            char c = chars[i];
            if (Character.isLetterOrDigit(c)) {
                chars[i] = (c == 'A') ? 'B' : 'A';
                return new String(chars);
            }
        }
        chars[firstColon + 1] = (chars[firstColon + 1] == 'A') ? 'B' : 'A';
        return new String(chars);
    }

    private boolean isAcceptedResponse(int responseCode, String responseBody) {
        return (responseCode >= 200) && (responseCode < 300)
                && (responseBody != null)
                && responseBody.contains("\"messageSigningResult\":\"VALID\"");
    }

    private boolean isRejectedResponse(int responseCode, String responseBody) {
        return (responseCode >= 400)
                || ((responseBody != null) && responseBody.contains("\"messageSigningResult\":\"INVALID\""))
                || ((responseBody != null) && responseBody.contains("\"status\":\"FAIL\""));
    }

    private void runMessageSigningBatchTest() {
        Log.d(TAG, "Starting installation message signing batch test with " + MESSAGE_SIGNING_BATCH_SIZE + " requests");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setVisibility(View.INVISIBLE);
            }
        });

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchResult batchResult = new BatchResult();

                for (int i = 0; i < MESSAGE_SIGNING_BATCH_SIZE; i++) {
                    boolean tamperSignature = (i % 2) == 1;
                    HttpsURLConnection connection = null;
                    String responseBody = null;
                    try {
                        URL url = new URL(getResources().getString(R.string.shapes_url));
                        connection = (HttpsURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setInstanceFollowRedirects(false);
                        connection.addRequestProperty("Accept", "application/json");
                        connection.addRequestProperty("Api-Key", getResources().getString(R.string.shapes_api_key));
                        ApproovService.addApproov(connection);

                        if (tamperSignature) {
                            String signature = connection.getRequestProperty("Signature");
                            connection.setRequestProperty("Signature", mutateSignatureHeader(signature));
                        }

                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        responseBody = readResponseBody(connection);

                        if (!tamperSignature) {
                            if (isAcceptedResponse(responseCode, responseBody))
                                batchResult.validAccepted++;
                            else if (isRejectedResponse(responseCode, responseBody))
                                batchResult.validRejected++;
                            else
                                batchResult.unexpectedResponses++;
                        } else {
                            if (isRejectedResponse(responseCode, responseBody))
                                batchResult.tamperedRejected++;
                            else if (isAcceptedResponse(responseCode, responseBody))
                                batchResult.tamperedAccepted++;
                            else
                                batchResult.unexpectedResponses++;
                        }

                        Log.d(TAG, "Batch request " + (i + 1) + "/" + MESSAGE_SIGNING_BATCH_SIZE
                                + " tampered=" + tamperSignature
                                + " code=" + responseCode
                                + " body=" + responseBody);
                    } catch (Exception e) {
                        batchResult.networkErrors++;
                        Log.e(TAG, "Batch request " + (i + 1) + "/" + MESSAGE_SIGNING_BATCH_SIZE
                                + " tampered=" + tamperSignature + " failed", e);
                    } finally {
                        if (connection != null)
                            connection.disconnect();
                    }
                }

                final String summary = String.format(
                        Locale.US,
                        "Batch test complete (%d requests)\nvalid accepted: %d\nvalid rejected: %d\ntampered accepted: %d\ntampered rejected: %d\nnetwork errors: %d\nunexpected: %d",
                        MESSAGE_SIGNING_BATCH_SIZE,
                        batchResult.validAccepted,
                        batchResult.validRejected,
                        batchResult.tamperedAccepted,
                        batchResult.tamperedRejected,
                        batchResult.networkErrors,
                        batchResult.unexpectedResponses
                );

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusImageView.setVisibility(View.GONE);
                        statusTextView.setText(summary);
                        statusView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        // find controls
        statusView = findViewById(R.id.viewStatus);
        statusImageView = (ImageView) findViewById(R.id.imgStatus);
        statusTextView = findViewById(R.id.txtStatus);
        helloCheckButton = findViewById(R.id.btnConnectionCheck);
        shapesCheckButton = findViewById(R.id.btnShapesCheck);

       // handle hello connection check
       helloCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // hide status
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusView.setVisibility(View.INVISIBLE);
                    }
                });

                // run our HTTP request in a background thread to avoid blocking the UI thread
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        // fetch from the endpoint
                        int imgId = R.drawable.confused;
                        String msg;
                        HttpsURLConnection connection = null;
                        try {
                            URL url = new URL(getResources().getString(R.string.hello_url));
                            connection = (HttpsURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.connect();
                            msg = "Http status code " + connection.getResponseCode();
                            if (connection.getResponseCode() == 200)
                                imgId = R.drawable.hello;
                        }
                        catch (IOException e) {
                            Log.d(TAG, "Hello call failed: " + e.toString());
                            msg = "Hello call failed: " + e.toString();
                        }
                        if (connection != null)
                            connection.disconnect();

                        // display the result
                        final int finalImgId = imgId;
                        final String finalMsg = msg;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusImageView.setImageResource(finalImgId);
                                statusTextView.setText(finalMsg);
                                statusView.setVisibility(View.VISIBLE);
                            }
                        });

                    }
                });
            }
        });

        // handle getting shapes
        shapesCheckButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            runMessageSigningBatchTest();
        }
    });
    }
}
