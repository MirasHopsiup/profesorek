package miras.slack.gpt;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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


}
