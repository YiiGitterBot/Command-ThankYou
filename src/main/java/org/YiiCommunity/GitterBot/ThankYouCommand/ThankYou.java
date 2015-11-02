package org.YiiCommunity.GitterBot.ThankYouCommand;

import com.amatkivskiy.gitter.rx.sdk.model.response.message.MessageResponse;
import com.amatkivskiy.gitter.rx.sdk.model.response.room.Mention;
import org.YiiCommunity.GitterBot.api.Command;
import org.YiiCommunity.GitterBot.containers.Gitter;
import org.YiiCommunity.GitterBot.models.database.CarmaHistory;
import org.YiiCommunity.GitterBot.models.database.User;
import org.YiiCommunity.GitterBot.utils.L;

import java.util.ArrayList;
import java.util.List;

public class ThankYou extends Command {
    private List<String> words;

    public ThankYou() {
        words = getConfig().getStringList("words");
    }

    @Override
    public void onMessage(MessageResponse message) {
        if (message.mentions.isEmpty())
            return;

        try {
            for (String item : words) {
                if (message.text.toLowerCase().startsWith(item) || message.text.toLowerCase().endsWith(item)) {
                    User giver = User.getUser(message.fromUser.username);

                    ArrayList<String> sent = new ArrayList<String>();
                    for (Mention mention : message.mentions) {
                        if (sent.contains(mention.screenName))
                            continue;

                        sent.add(mention.screenName);

                        if (mention.screenName.equals(message.fromUser.username)) {
                            Gitter.sendMessage(getConfig().getString("messages.selfThanks", "*@{username} selflike? How vulgar!*").replace("{username}", message.fromUser.username));
                            continue;
                        }
                        User receiver = User.getUser(mention.screenName);

                        if (System.currentTimeMillis() / 1000 - CarmaHistory.getLastThankYou(giver, receiver) < getConfig().getInt("minTimeBetweenThanks")) {
                            Gitter.sendMessage(
                                    getConfig()
                                            .getString("messages.tooSoon", "*Thanks (+1) to @{receiver} accepted! Now his carma **{carma}**.*")
                                            .replace("{receiver}", receiver.getUsername())
                                            .replace("{giver}", message.fromUser.username)
                            );
                            return;
                        }


                        receiver.changeCarma(1, giver, message.text);
                        Gitter.sendMessage(
                                getConfig()
                                        .getString("messages.thanks", "*Thanks (+1) to @{receiver} accepted! Now his carma **{carma}**.*")
                                        .replace("{receiver}", receiver.getUsername())
                                        .replace("{giver}", message.fromUser.username)
                                        .replace("{carma}", (receiver.getCarma() >= 0 ? "+" : "-") + receiver.getCarma())
                        );
                        receiver.updateAchievements();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            L.$(e.getMessage());
        }
    }
}
