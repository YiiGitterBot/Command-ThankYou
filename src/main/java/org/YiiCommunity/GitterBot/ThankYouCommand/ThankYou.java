package org.YiiCommunity.GitterBot.ThankYouCommand;

import com.amatkivskiy.gitter.sdk.model.response.UserResponse;
import com.amatkivskiy.gitter.sdk.model.response.message.MessageResponse;
import com.amatkivskiy.gitter.sdk.model.response.room.Mention;
import com.amatkivskiy.gitter.sdk.model.response.room.RoomResponse;
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
    public void onMessage(RoomResponse room, MessageResponse message) {
        if (message.mentions.isEmpty())
            return;

        try {
            String text = message.text.replaceAll("[^a-zA-Zа-яА-Я0-9@\\s]", "").toLowerCase();
            for (String item : words) {
                if (text.startsWith(item) || text.endsWith(item)) {
                    User giver = User.getUser(message.fromUser.username);

                    ArrayList<String> sent = new ArrayList<>();
                    for (Mention mention : message.mentions) {
                        if (sent.contains(mention.screenName))
                            continue;

                        sent.add(mention.screenName);

                        if (mention.screenName.equals(message.fromUser.username)) {
                            Gitter.sendMessage(room, getConfig().getString("messages.selfThanks", "*@{username} selflike? How vulgar!*").replace("{username}", message.fromUser.username));
                            continue;
                        }

                        if (getConfig().getBoolean("denyThanksIfNotInRoom", false)) {
                            boolean exists = false;
                            for (UserResponse userItem : Gitter.getUsersInRoom(room.id)) {
                                if (userItem.username.equalsIgnoreCase(mention.screenName))
                                    exists = true;
                            }

                            if (!exists) {
                                Gitter.sendMessage(room,
                                        getConfig()
                                                .getString("messages.userNotFound", "User @{username} not found!")
                                                .replace("{username}", mention.screenName)
                                                .replace("{giver}", message.fromUser.username)
                                );
                                continue;
                            }
                        }

                        User receiver = User.getUser(mention.screenName);
                        if (receiver == null) {
                            Gitter.sendMessage(room,
                                    getConfig()
                                            .getString("messages.userNotFound", "User @{username} not found!")
                                            .replace("{username}", mention.screenName)
                                            .replace("{giver}", message.fromUser.username)
                            );
                            continue;
                        }

                        if (System.currentTimeMillis() / 1000 - CarmaHistory.getLastThankYou(giver, receiver) < getConfig().getInt("minTimeBetweenThanks", 60)) {
                            Gitter.sendMessage(room,
                                    getConfig()
                                            .getString("messages.tooSoon", "You tried to change @{receiver} carma too often. Canceling.")
                                            .replace("{receiver}", receiver.getUsername())
                                            .replace("{giver}", message.fromUser.username)
                            );
                            return;
                        }


                        receiver.changeCarma(1, giver, message.text, room);
                        Gitter.sendMessage(room,
                                getConfig()
                                        .getString("messages.thanks", "*Thanks (+1) to @{receiver} accepted! Now his carma **{carma}**.*")
                                        .replace("{receiver}", receiver.getUsername())
                                        .replace("{giver}", message.fromUser.username)
                                        .replace("{carma}", (receiver.getCarma() >= 0 ? "+" : "-") + receiver.getCarma())
                        );
                        receiver.updateAchievements(room);
                        giver.updateAchievements(room);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            L.$(e.getMessage());
        }
    }
}
