package miras.slack.gpt;


import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.view.Views.view;
import static miras.slack.gpt.BotConstants.PROGRAMMING_CHANNEL_ID;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.service.builtin.FileOAuthStateService;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageChannelJoinEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppStarter {

    static Logger log = LoggerFactory.getLogger("app");

    static String key = System.getenv("OPENAI_KEY");

    public static void main(String[] args)throws Exception  {
/*
        var config = new AppConfig();
        config.setClientId("766129040951.5229037240359");
        config.setClientSecret("bf333881fbc9550085437626b7f40162");
        config.setSigningSecret("d40e516ed80a67ae64dd9671077045b5");
        config.setSingleTeamBotToken("xoxb-766129040951-5263042410257-8rJimeq49LKH3vV49Z4y8ARN");
        var app = new App(config);
*/

        var app = new App();
        var service = new OpenAiService(key);
        var gptResponse = new GptResponse(service);

       /* config.setSingleTeamBotToken(System.getenv("SLACK_BOT_TOKEN"));
        config.setSigningSecret(System.getenv("SLACK_SIGNING_SECRET"));
        */

        app.command("/poem", (payload, ctx) -> {
            var text = payload.getPayload().getText();
            var chatRequest = new WelcomePrompt().createPoemPrompt(text);

            ctx.respond(r -> r.text(">Poem about " + text));
            gptResponse.getResponseWithUpdates(chatRequest, payload, ctx)
                .subscribe(response -> {
                    log.info("response: {}", response);
                }, t -> {
                    log.info("error, {}", t.getMessage());
                    ctx.client().chatPostMessage(r -> r
                        .channel(ctx.getChannelId())
                        .text("sorry error poszedł"));
                });

            return ctx.ack();
        });

        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            var appHomeView = view(view -> view
                .type("home")
                .blocks(asBlocks(
                    section(section -> section.text(markdownText(mt -> mt.text("*I'm totally useless bot* :tada:")))),
                    divider(),
                    section(section -> section.text(markdownText(mt -> mt.text(
                        "I give useless programming tips."))))
                ))
            );

            var res = ctx.client().viewsPublish(r -> r
                .userId(payload.getEvent().getUser())
                .view(appHomeView)
            );

            return ctx.ack();
        });

        app.event(MessageEvent.class, (payload, ctx) -> {
            log.info("message event: {}", payload.getEvent());

            // add a reaction to the message
           /*ctx.client().reactionsAdd(r -> r
                .channel(payload.getEvent().getChannel())
                .name("smile")
                .timestamp(payload.getEvent().getTs())
            );*/


            ChatCompletionRequest chatRequest;

            switch (payload.getEvent().getChannel()) {
                case PROGRAMMING_CHANNEL_ID: {
                    var programmingPrompt = new ProgramminPrompt(payload, ctx);
                    chatRequest = programmingPrompt.createProgrammingPrompt();
                    break;
                }
                default: {
                    var philosophyPrompt = new PhilosophyPrompt(payload, ctx);
                    chatRequest = philosophyPrompt.createPhilosophicPrompt();
                }
            }

            gptResponse.getResponseWithUpdates(chatRequest, payload, ctx)
                .subscribe(response -> {
                    log.info("response: {}", response);

                    /*
                    ctx.client().chatPostMessage(r -> r
                        .channel(payload.getEvent().getChannel())
                        .threadTs(payload.getEvent().getTs())
                        .text(response)
                    );
                     */
                }, t -> {
                    log.info("error, {}", t.getMessage());
                    ctx.client().chatPostMessage(r -> r
                        .channel(payload.getEvent().getChannel())
                        .threadTs(payload.getEvent().getTs())
                        .text("sorry error poszedł"));
                });

            log.info("ack");
            return ctx.ack();
        });

        app.event(MessageChangedEvent.class, (payload, ctx) -> {
            log.info("message changed event: {}", payload.getEvent().getMessage().getText());
            return ctx.ack();
        });

        app.event(MessageDeletedEvent.class, (payload, ctx) -> {
            log.info("message deleted event: {}", payload.getEvent().getPreviousMessage().getText());
            return ctx.ack();
        });

        app.event(ReactionAddedEvent.class, (payload, ctx) -> {
            log.info("reaction added event: {}", payload.getEvent().getReaction());
            return ctx.ack();
        });

        app.event(MessageChannelJoinEvent.class, (payload, ctx) -> {
            log.info("joined channel event: {}", payload.getEvent().getUser());

            var userInfo =
                ctx.client().usersInfo(r -> r.user(payload.getEvent().getUser()));
            log.info("user details: {}", userInfo);

            ChatCompletionRequest chatRequest;
            var welcomePrompt = new WelcomePrompt();
            switch (payload.getEvent().getChannel()) {
                case PROGRAMMING_CHANNEL_ID: {
                    chatRequest = welcomePrompt.createWelcomeProgrammerPrompt(userInfo.getUser().getRealName());
                }
                default: {
                    chatRequest = welcomePrompt.createPhilosophyClientPrompt(userInfo.getUser().getRealName());
                }
            }

            gptResponse.getResponse(chatRequest)
                .subscribe(response -> {
                        log.info("response: {}", response);
                        ctx.client().chatPostMessage(r -> r
                            .channel(payload.getEvent().getChannel())
                            .text(response)
                        );
                    });

            /*
            ctx.client().chatPostMessage(r -> r
                .channel(payload.getEvent().getChannel())
                .text("Another one that can't code....., meh. Welcome or something. :robot_face:")
            );
             */

            return ctx.ack();
        });

        app.blockAction("button_1", (req, ctx) -> {
            log.info("button clicked: {}", req.getPayload().getActions());
            //ctx.respond("You clicked a button!");
            return ctx.ack();
        });

        log.info("Starting app...");

        // ---- oauth ----
        App oauthApp = new App().asOAuthApp(true);
        log.info("config {}", oauthApp.config());
        oauthApp.service(new FileOAuthStateService(oauthApp.config()));

        oauthApp.endpoint("GET", "/slack/oauth/completion", (req, ctx) -> {
            return Response.builder()
                .statusCode(200)
                .contentType("text/html")
                .body("ok")
                .build();
        });

        oauthApp.endpoint("GET", "/slack/oauth/cancellation", (req, ctx) -> {
            return Response.builder()
                .statusCode(200)
                .contentType("text/html")
                .body("ok")
                .build();
        });

        //var server = new SlackAppServer(app);
        SlackAppServer server = new SlackAppServer(new HashMap<>(Map.of(
            "/slack/events", app,
            "/slack/oauth", oauthApp
        )));
        server.start();




    }


}
