/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.internal.http;

import java.io.IOException;
import java.net.ProtocolException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.StreamAllocation;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

/** This is the last interceptor in the chain. It makes a network call to the server. */
public final class CallServerInterceptor implements Interceptor {
  private final boolean forWebSocket;

  public CallServerInterceptor(boolean forWebSocket) {
    this.forWebSocket = forWebSocket;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    HttpStream httpStream = ((RealInterceptorChain) chain).httpStream();
    StreamAllocation streamAllocation = ((RealInterceptorChain) chain).streamAllocation();
    Request request = chain.request();

    long sentRequestMillis = System.currentTimeMillis();
    httpStream.writeRequestHeaders(request);

    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
      Sink requestBodyOut = httpStream.createRequestBody(request, request.body().contentLength());
      BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
      request.body().writeTo(bufferedRequestBody);
      bufferedRequestBody.close();
    }

    httpStream.finishRequest();

    Response response = httpStream.readResponseHeaders()
        .request(request)
        .handshake(streamAllocation.connection().handshake())
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    if (!forWebSocket || response.code() != 101) {
      response = response.newBuilder()
          .body(httpStream.openResponseBody(response))
          .build();
    }

    if ("close".equalsIgnoreCase(response.request().header("Connection"))
        || "close".equalsIgnoreCase(response.header("Connection"))) {
      streamAllocation.noNewStreams();
    }

    int code = response.code();
    if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
      throw new ProtocolException(
          "HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
    }

    return response;
  }
}
