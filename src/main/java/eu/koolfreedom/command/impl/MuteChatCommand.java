package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "mutechat", description = "Mutes the chat for non-staff")
public class MuteChatCommand extends KoolCommand
{
    @Setter
    @Getter
    private static boolean chatMuted = false;

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx ->
        {
            chatMuted = !chatMuted;
            String status = chatMuted ? "Muting" : "Unmuting";
            FUtil.staffAction(sender(ctx), status + " global chat");
        }));
    }
}
