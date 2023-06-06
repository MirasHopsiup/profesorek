package miras.slack.gpt;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.model.event.MessageEvent;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

    Single<String> getResponseWithUpdates(ChatCompletionRequest chatRequest, EventsApiPayload<MessageEvent> payload,
        EventContext ctx) {

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
            .buffer(2, TimeUnit.SECONDS)
            .map(respChunks -> {

                var chunk = String.join(" ", respChunks);

                if (StringUtils.isBlank(messageId.get())) {
                    var postResponse = ctx.client().chatPostMessage(r -> r
                        .channel(payload.getEvent().getChannel())
                        .threadTs(payload.getEvent().getTs())
                        .text(chunk));

                    messageId.set(postResponse.getTs());
                    lastResponse.set(chunk);
                }
                else {
                    var updatedMessage = lastResponse.get() + chunk;
                    ctx.client().chatUpdate(r -> r
                        .channel(payload.getEvent().getChannel())
                        .ts(messageId.get())
                        .text(updatedMessage));
                    lastResponse.set(updatedMessage);
                }

                return lastResponse.get();
            })
            .last("");
    }


}
