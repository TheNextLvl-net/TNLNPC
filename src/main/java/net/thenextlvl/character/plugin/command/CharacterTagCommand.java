package net.thenextlvl.character.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.character.plugin.CharacterPlugin;
import net.thenextlvl.character.plugin.command.suggestion.CharacterSuggestionProvider;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
class CharacterTagCommand {
    static LiteralArgumentBuilder<CommandSourceStack> create(CharacterPlugin plugin) {
        return Commands.literal("tag")
                .then(reset(plugin))
                .then(set(plugin))
                .then(toggle(plugin));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> reset(CharacterPlugin plugin) {
        return Commands.literal("reset").then(characterArgument(plugin)
                .executes(context -> reset(context, plugin)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> set(CharacterPlugin plugin) {
        return Commands.literal("set").then(characterArgument(plugin)
                .then(tagArgument(plugin).executes(context -> set(context, plugin))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> toggle(CharacterPlugin plugin) {
        return Commands.literal("toggle").then(characterArgument(plugin)
                .executes(context -> toggle(context, plugin)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> characterArgument(CharacterPlugin plugin) {
        return Commands.argument("character", StringArgumentType.word())
                .suggests(new CharacterSuggestionProvider(plugin));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> tagArgument(CharacterPlugin plugin) {
        return Commands.argument("tag", StringArgumentType.greedyString());
    }

    private static int set(CommandContext<CommandSourceStack> context, CharacterPlugin plugin) {
        var sender = context.getSource().getSender();
        var name = context.getArgument("character", String.class);
        var tag = context.getArgument("tag", String.class);
        var character = plugin.characterController().getCharacter(name).orElse(null);
        var displayName = MiniMessage.miniMessage().deserialize(tag);

        if (character == null) {
            plugin.bundle().sendMessage(sender, "character.not_found", Placeholder.unparsed("name", name));
            return 0;
        } else if (Objects.equals(character.getDisplayName(), displayName)) {
            plugin.bundle().sendMessage(sender, "nothing.changed");
            return 0;
        }

        character.setDisplayName(displayName);
        plugin.bundle().sendMessage(sender, "character.tag.set",
                Placeholder.unparsed("character", name),
                Placeholder.component("tag", displayName));
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> context, CharacterPlugin plugin) {
        var sender = context.getSource().getSender();
        var name = context.getArgument("character", String.class);
        var character = plugin.characterController().getCharacter(name).orElse(null);

        if (character == null) {
            plugin.bundle().sendMessage(sender, "character.not_found", Placeholder.unparsed("name", name));
            return 0;
        } else if (character.getDisplayName() == null) {
            plugin.bundle().sendMessage(sender, "nothing.changed");
            return 0;
        }

        character.setDisplayName(null);
        plugin.bundle().sendMessage(sender, "character.tag.reset", Placeholder.unparsed("character", name));
        return Command.SINGLE_SUCCESS;
    }

    private static int toggle(CommandContext<CommandSourceStack> context, CharacterPlugin plugin) {
        var sender = context.getSource().getSender();
        var name = context.getArgument("character", String.class);
        var character = plugin.characterController().getCharacter(name).orElse(null);

        if (character == null) {
            plugin.bundle().sendMessage(sender, "character.not_found", Placeholder.unparsed("name", name));
            return 0;
        }

        var message = character.isDisplayNameVisible() ? "character.tag.hidden" : "character.tag.shown";
        character.setDisplayNameVisible(!character.isDisplayNameVisible());
        plugin.bundle().sendMessage(sender, message, Placeholder.unparsed("character", name));
        return Command.SINGLE_SUCCESS;
    }
}
