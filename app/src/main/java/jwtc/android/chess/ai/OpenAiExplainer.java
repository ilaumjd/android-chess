package jwtc.android.chess.ai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends the current position to the OpenAI Chat Completions API and returns a
 * plain-language explanation. The model only sees the FEN and the move list the
 * app already has; it is asked not to invent moves.
 */
public final class OpenAiExplainer {

    public interface Callback {
        void onResult(String text);

        void onError(String message);
    }

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private OpenAiExplainer() {
    }

    /**
     * Runs the request on a background thread. Callbacks are delivered on the main thread.
     */
    public static void explain(final String apiKey, final String model, final String fen,
                               final String moveList, final Callback callback) {
        new Thread(() -> {
            try {
                String text = call(apiKey, model, fen, moveList);
                mainHandler.post(() -> callback.onResult(text));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                mainHandler.post(() -> callback.onError(msg));
            }
        }, "openai-explain").start();
    }

    private static String call(String apiKey, String model, String fen, String moveList) throws Exception {
        String system = "You are a chess coach explaining a position to a club player. "
                + "Use only the FEN and move list provided. Do not invent moves that are not legal in the position. "
                + "Explain: the material balance, the key threats for each side, the main plan, "
                + "and one or two candidate ideas. Keep it under 150 words, plain language, no headings.";

        String user = "FEN: " + fen + "\n"
                + "Moves so far (SAN): " + (moveList == null || moveList.trim().isEmpty() ? "(none)" : moveList.trim());

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", system))
                .put(new JSONObject().put("role", "user").put("content", user));

        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", messages);

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI error " + response.code() + ": " + shortError(raw));
            }
            JSONObject json = new JSONObject(raw);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        }
    }

    private static String shortError(String raw) {
        try {
            return new JSONObject(raw).getJSONObject("error").getString("message");
        } catch (Exception e) {
            return raw.length() > 200 ? raw.substring(0, 200) : raw;
        }
    }
}
