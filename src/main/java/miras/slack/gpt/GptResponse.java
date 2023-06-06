package miras.slack.gpt;

import com.slack.api.methods.MethodsClient;
import com.slack.api.util.http.SlackHttpClient;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class GptResponse {

    final OpenAiService service;
    Single<String> getResponse(ChatCompletionRequest chatRequest) {
        return service.streamChatCompletion(chatRequest)
            .subscribeOn(Schedulers.computation())
            .map(response -> Optional.ofNullable(
                response.getChoices().get(0).getMessage().getContent()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .doOnNext(response -> log.info("... : {}", response))
            .reduce(
                new StringBuffer(),
                StringBuffer::append)
            .map(StringBuffer::toString);
    }

    Single<String> getResponseWithUpdates(ChatCompletionRequest chatRequest, MethodsClient client, String channelId) {

        AtomicReference<String> messageId = new AtomicReference<>("");
        AtomicReference<String> lastResponse = new AtomicReference<>("");

        return
        service.streamChatCompletion(chatRequest)
            .subscribeOn(Schedulers.computation())
            .map(response -> Optional.ofNullable(
                response.getChoices().get(0).getMessage().getContent()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .doOnNext(response -> log.info("... : {}", response))
            .buffer(1, TimeUnit.SECONDS)
            .map(respChunks -> {

                var chunk = String.join(" ", respChunks);

                if (StringUtils.isBlank(messageId.get())) {
                    var postResponse = client.chatPostMessage(r -> r
                        .channel(channelId)
                        .text(chunk));

                    messageId.set(postResponse.getMessage().getTs());
                    lastResponse.set(chunk);
                }
                else {
                    var updatedMessage = lastResponse.get() + " " + chunk;
                    client.chatUpdate(r -> r
                        .channel(channelId)
                        .ts(messageId.get())
                        .text(updatedMessage));
                    lastResponse.set(updatedMessage);
                }

                return lastResponse.get();
            })
            .last("");
    }


}
