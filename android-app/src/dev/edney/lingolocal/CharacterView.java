package dev.edney.lingolocal;

import android.content.Context;
import android.graphics.Color;
import android.webkit.*;
import java.io.*;
import java.util.Collections;

/** Rive e animações embarcados: nenhuma requisição externa é permitida. */
final class CharacterView extends WebView {
    CharacterView(Context context,String name) {
        super(context); setBackgroundColor(Color.TRANSPARENT); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        getSettings().setJavaScriptEnabled(true); getSettings().setAllowFileAccess(false);
        getSettings().setAllowContentAccess(false); getSettings().setBlockNetworkLoads(true);
        setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                String path=request.getUrl().getPath();
                if(!"appassets.androidplatform.net".equals(request.getUrl().getHost()) || path==null || !path.matches("/[a-zA-Z0-9_.-]+")) return empty();
                try {
                    String mime=path.endsWith(".wasm")?"application/wasm":path.endsWith(".js")?"application/javascript":path.endsWith(".html")?"text/html":"application/octet-stream";
                    return new WebResourceResponse(mime,null,200,"OK",Collections.singletonMap("Access-Control-Allow-Origin","https://appassets.androidplatform.net"),getContext().getAssets().open(path.substring(1)));
                } catch(IOException ex) { return empty(); }
            }
            private WebResourceResponse empty() { return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(new byte[0])); }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) { return true; }
        });
        setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) { android.util.Log.i("LingoCharacter",message.message()); return true; }
        });
        loadUrl("https://appassets.androidplatform.net/character.html?name="+name);
    }
    void dispose() { stopLoading(); loadUrl("about:blank"); destroy(); }
    void feedback(boolean correct) { evaluateJavascript("window.lingoFeedback&&window.lingoFeedback("+(correct?"true":"false")+")",null); }
}
