package org.YiiCommunity.GitterBot.ThankYouCommand;

import com.amatkivskiy.gitter.rx.sdk.model.response.message.MessageResponse;
import com.amatkivskiy.gitter.rx.sdk.model.response.room.Mention;
import org.YiiCommunity.GitterBot.api.Command;
import org.YiiCommunity.GitterBot.containers.Gitter;
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
                            Gitter.sendMessage("*@" + message.fromUser.username + " самолайк? Как вульгарно!*");
                            continue;
                        }
                        User receiver = User.getUser(mention.screenName);
                        receiver.changeCarma(1, giver, message.text);
                        Gitter.sendMessage("*Спасибо (+1) для @" + receiver.getUsername() + " принято! Текущая карма **" + (receiver.getCarma() >= 0 ? "+" : "-") + receiver.getCarma() + "**.*");
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
