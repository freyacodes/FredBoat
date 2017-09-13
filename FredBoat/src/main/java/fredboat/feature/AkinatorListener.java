/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.feature;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.FredBoat;
import fredboat.event.UserListener;
import fredboat.messaging.internal.Context;
import fredboat.messaging.internal.LeakSafeContext;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AkinatorListener extends UserListener {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final String NEW_SESSION_URL = "http://api-en4.akinator.com/ws/new_session?partner=1";
    private final String ANSWER_URL = "http://api-en4.akinator.com/ws/answer";
    private final String GET_GUESS_URL = "http://api-en4.akinator.com/ws/list";
    private final String CHOICE_URL = "http://api-en4.akinator.com/ws/choice";
    private final String EXCLUSION_URL = "http://api-en4.akinator.com/ws/exclusion";

    private final LeakSafeContext context;
    private final String channelId;
    private final String userId;
    private StepInfo stepInfo;

    private final String signature;
    private final String session;
    private Guess guess;
    private boolean lastQuestionWasGuess = false;

    private long lastActionReceived = System.currentTimeMillis();
    private Future timeoutTask;


    public AkinatorListener(Context context) throws UnirestException {
        this.context = new LeakSafeContext(context);
        this.userId = context.getMember().getUser().getId();
        this.channelId = context.getTextChannel().getId();

        context.sendTyping();

        //Start new session
        JSONObject json = Unirest.get(NEW_SESSION_URL)
                .queryString("player", RandomStringUtils.random(16))
                .asJson().getBody().getObject();
        stepInfo = new StepInfo(json);

        signature = stepInfo.getSignature();
        session = stepInfo.getSession();

        sendNextQuestion();
        timeoutTask = scheduler.scheduleAtFixedRate(this::checkTimeout, 1, 1, TimeUnit.MINUTES);
    }

    private void checkTimeout() {
        if (System.currentTimeMillis() - lastActionReceived > TimeUnit.MINUTES.toMillis(5)) {
            FredBoat.getListenerBot().removeListener(userId);
            timeoutTask.cancel(false);
        }
    }

    private void sendNextQuestion() {
        String name = context.getMember().getEffectiveName();
        String out = "**" + name + ": Question " + (stepInfo.getStepNum() + 1) + "**\n"
                + stepInfo.getQuestion() + "\n [yes/no/idk/probably/probably not]";
        context.reply(out);
        lastQuestionWasGuess = false;
    }

    private void sendGuess() throws UnirestException {
        guess = new Guess();
        String out = "Is this your character?\n" + guess.toString() + "\n[yes/no]";
        context.reply(out);
        lastQuestionWasGuess = true;
    }

    private void answerQuestion(byte answer) {
        try {
            JSONObject json = Unirest.get(ANSWER_URL)
                    .queryString("session", session)
                    .queryString("signature", signature)
                    .queryString("step", stepInfo.getStepNum())
                    .queryString("answer", answer)
                    .asJson().getBody().getObject();
            stepInfo = new StepInfo(json);

            if (stepInfo.gameOver) {
                context.reply("Bravo !\n"
                        + "You have defeated me !\n"
                        + "<http://akinator.com>");
                FredBoat.getListenerBot().removeListener(userId);
                return;
            }

            if (stepInfo.getProgression() > 90) {
                sendGuess();
            } else {
                sendNextQuestion();
            }
        } catch (UnirestException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void answerGuess(byte answer) {
        try {
            if (answer == 0) {
                Unirest.get(CHOICE_URL)
                        .queryString("session", session)
                        .queryString("signature", signature)
                        .queryString("step", stepInfo.getStepNum())
                        .queryString("element", guess.getId())
                        .asString();
                context.reply("Great ! Guessed right one more time.\n"
                        + "I love playing with you!\n"
                        + "<http://akinator.com>");
                FredBoat.getListenerBot().removeListener(userId);
            } else if (answer == 1) {
                Unirest.get(EXCLUSION_URL)
                        .queryString("session", session)
                        .queryString("signature", signature)
                        .queryString("step", stepInfo.getStepNum())
                        .queryString("forward_answer", answer)
                        .asString();

                lastQuestionWasGuess = false;
                sendNextQuestion();
            }
        } catch (UnirestException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Channel channel = event.getChannel();

        if (!channel.getId().equals(channelId)) {
            return;
        }

        byte answer;
        //<editor-fold defaultstate="collapsed" desc="switch">
        switch (event.getMessage().getStrippedContent().trim().toLowerCase()) {
            case "yes":
                answer = 0;
                break;
            case "y":
                answer = 0;
                break;
            case "no":
                answer = 1;
                break;
            case "n":
                answer = 1;
                break;
            case "idk":
                answer = 2;
                break;
            case "p":
                answer = 3;
                break;
            case "probably":
                answer = 3;
                break;
            case "pn":
                answer = 4;
                break;
            case "probably not":
                answer = 4;
                break;
            default:
                answer = -1;
                break;
        }
//</editor-fold>

        if (answer == -1) {
            return;
        }

        lastActionReceived = System.currentTimeMillis();

        if (lastQuestionWasGuess) {
            if (answer != 0 && answer != 1) {
                return;
            }

            context.sendTyping();
            answerGuess(answer);
        } else {
            context.sendTyping();
            answerQuestion(answer);
        }
    }

    private class StepInfo {

        private boolean gameOver;
        private String signature = "";
        private String session = "";
        private String question;
        private int stepNum;
        private double progression;

        StepInfo(JSONObject json) {
            String completion = json.getString("completion");
            if ("OK".equalsIgnoreCase(completion)) {
                JSONObject params = json.getJSONObject("parameters");
                JSONObject info = params.has("step_information") ? params.getJSONObject("step_information") : params;
                question = info.getString("question");
                stepNum = info.getInt("step");
                progression = info.getDouble("progression");

                JSONObject identification = params.optJSONObject("identification");
                if (identification != null) {
                    signature = identification.getString("signature");
                    session = identification.getString("session");
                }
                gameOver = false;
            } else {
                gameOver = true;
            }
        }

        String getQuestion() {
            return question;
        }

        int getStepNum() {
            return stepNum;
        }

        String getSignature() {
            return signature;
        }

        String getSession() {
            return session;
        }

        double getProgression() {
            return progression;
        }

    }

    private class Guess {

        private final String id;
        private final String name;
        private final String desc;
        private final int ranking;
        private final String pseudo;
        private final String imgPath;

        Guess() throws UnirestException {
            JSONObject json = Unirest.get(GET_GUESS_URL)
                    .queryString("session", session)
                    .queryString("signature", signature)
                    .queryString("step", stepInfo.getStepNum())
                    .asJson()
                    .getBody()
                    .getObject();

            JSONObject character = json.getJSONObject("parameters")
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject("element");

            id = character.getString("id");
            name = character.getString("name");
            desc = character.getString("description");
            ranking = character.getInt("ranking");
            pseudo = character.getString("pseudo");
            imgPath = character.getString("absolute_picture_path");
        }

        public String getDesc() {
            return desc;
        }

        public String getImgPath() {
            return imgPath;
        }

        public String getName() {
            return name;
        }

        public String getPseudo() {
            return pseudo;
        }

        public int getRanking() {
            return ranking;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            String str = "**" + name + "**\n"
                    //+ (!pseudo.equals("none") ? "(" + pseudo + ")\n" : "")
                    + desc + "\n"
                    + "Ranking as **#" + ranking + "**\n"
                    + imgPath;
            return str;
        }

    }

}
