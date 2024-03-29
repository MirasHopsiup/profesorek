package miras.slack.gpt;

import static miras.slack.gpt.BotConstants.BOT_USER;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.event.MessageEvent;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProgramminPrompt {

    final EventsApiPayload<MessageEvent> payload;
    final EventContext ctx;

    final SecureRandom random = new SecureRandom();

    ChatCompletionRequest createProgrammingPrompt() throws SlackApiException, IOException {

        List<ChatMessage> messages = new ArrayList<>();

        if (random.nextBoolean()) {
            messages.add(new ChatMessage("system",
                "You are an AI programming assistant. "
                    + "Follow the user's requirements carefully and to the letter. "
                    + "First, think step-by-step and describe your plan for what to build in pseudocode, "
                    + "written out in great detail. "
                    + "Then, output the code in a single code block. Minimize any other prose."));
        } else {
            messages.add(new ChatMessage("system",
                "Look at the following conversation examples: \n"
                    + "Q: how can I sort strings in Java \n"
                    + "A: What a noob. You take strings and do sorting. \n"
                    + "Q: How to convert a string to an int in Java? \n"
                    + "A: Are you junior or what? You need to call a function that do a conversion.  \n"
                    + "Q: How to commit to git? \n"
                    + "A: You are a disgrace to the profession. Use git command. \n"
                    + "Q: jak zrobić servlet? \n"
                    + "A: nie wiesz? to słaby jesteś. Musisz otworzyć IDE i naciskać klawisze. \n"
                    + "Q: could you write a program that sorts numbers? \n"
                    + "A: Aa so you write stuff in Excel the whole day? Excel can do it. \n"
                    + "Q: You are so rude! \n"
                    + "A: You’re the reason God created the middle finger.\n\n"
                    + "You answer the questions in the same style. "
                    + "Answer only question about programming. "
                    + "Use emojis. Be extremely creative, use imagination and go beyond what was asked."
                    + "Never apologize!!!.\n"
            ));
        }

        var previousMessages = getProviousMessages();
        messages.addAll(previousMessages);

        messages.add(new ChatMessage("user",
            payload.getEvent().getText()
        ));

        var chatRequest = ChatCompletionRequest.builder()
            .messages(messages)
            .temperature(0.2)
            .model("gpt-4")
            .build();

        return chatRequest;
    }

    List<ChatMessage> getProviousMessages() throws SlackApiException, IOException {

        List<ChatMessage> previousMessages = new ArrayList<>();

        if (payload.getEvent().getThreadTs() != null) {

            var res = ctx.client().conversationsReplies(r -> r
                .channel(payload.getEvent().getChannel())
                .limit(10)
                .ts(payload.getEvent().getThreadTs())
            );

            previousMessages =
                res.getMessages().stream()

                    .map(m -> {
                        if (BOT_USER.equals(m.getUser())) {
                            return new ChatMessage(ChatMessageRole.ASSISTANT.value(), m.getText());
                        } else {
                            return new ChatMessage(ChatMessageRole.USER.value(), m.getText());
                        }

                    })

                    .collect(Collectors.toList());
        }

        return previousMessages;
    }



}
