package net.thenextlvl.character.plugin;

import com.destroystokyo.paper.profile.ProfileProperty;
import core.i18n.file.ComponentBundle;
import core.io.IO;
import core.nbt.NBTInputStream;
import core.nbt.serialization.NBT;
import core.nbt.serialization.ParserException;
import core.nbt.tag.CompoundTag;
import core.paper.messenger.PluginMessenger;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.thenextlvl.character.Character;
import net.thenextlvl.character.CharacterProvider;
import net.thenextlvl.character.PlayerCharacter;
import net.thenextlvl.character.action.ActionType;
import net.thenextlvl.character.action.ClickAction;
import net.thenextlvl.character.plugin.character.PaperCharacterController;
import net.thenextlvl.character.plugin.character.PaperSkinFactory;
import net.thenextlvl.character.plugin.character.action.PaperActionType;
import net.thenextlvl.character.plugin.character.action.PaperActionTypeProvider;
import net.thenextlvl.character.plugin.character.attribute.PaperAttributeProvider;
import net.thenextlvl.character.plugin.command.CharacterCommand;
import net.thenextlvl.character.plugin.listener.CharacterListener;
import net.thenextlvl.character.plugin.listener.ConnectionListener;
import net.thenextlvl.character.plugin.listener.EntityListener;
import net.thenextlvl.character.plugin.listener.test;
import net.thenextlvl.character.plugin.serialization.ActionTypeAdapter;
import net.thenextlvl.character.plugin.serialization.AddressAdapter;
import net.thenextlvl.character.plugin.serialization.BrightnessAdapter;
import net.thenextlvl.character.plugin.serialization.CharacterSerializer;
import net.thenextlvl.character.plugin.serialization.ClickActionAdapter;
import net.thenextlvl.character.plugin.serialization.ComponentAdapter;
import net.thenextlvl.character.plugin.serialization.EntityTypeAdapter;
import net.thenextlvl.character.plugin.serialization.ItemStackAdapter;
import net.thenextlvl.character.plugin.serialization.KeyAdapter;
import net.thenextlvl.character.plugin.serialization.LocationAdapter;
import net.thenextlvl.character.plugin.serialization.NamedTextColorAdapter;
import net.thenextlvl.character.plugin.serialization.ProfilePropertyAdapter;
import net.thenextlvl.character.plugin.serialization.SoundAdapter;
import net.thenextlvl.character.plugin.serialization.TitleAdapter;
import net.thenextlvl.character.plugin.serialization.TitleTimesAdapter;
import net.thenextlvl.character.plugin.serialization.Vector3fAdapter;
import net.thenextlvl.character.plugin.serialization.WorldAdapter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.READ;

@NullMarked
public class CharacterPlugin extends JavaPlugin implements CharacterProvider {
    public static final String ISSUES = "https://github.com/TheNextLvl-net/characters/issues/new";
    private final Metrics metrics = new Metrics(this, 24223);
    private final File savesFolder = new File(getDataFolder(), "saves");
    private final File translations = new File(getDataFolder(), "translations");

    private final NBT nbt = NBT.builder()
            .registerTypeHierarchyAdapter(ActionType.class, new ActionTypeAdapter(this))
            .registerTypeHierarchyAdapter(Brightness.class, new BrightnessAdapter())
            .registerTypeHierarchyAdapter(Character.class, new CharacterSerializer())
            .registerTypeHierarchyAdapter(ClickAction.class, new ClickActionAdapter())
            .registerTypeHierarchyAdapter(Component.class, new ComponentAdapter())
            .registerTypeHierarchyAdapter(EntityType.class, new EntityTypeAdapter())
            .registerTypeHierarchyAdapter(InetSocketAddress.class, new AddressAdapter())
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackAdapter())
            .registerTypeHierarchyAdapter(Key.class, new KeyAdapter())
            .registerTypeHierarchyAdapter(Location.class, new LocationAdapter())
            .registerTypeHierarchyAdapter(NamedTextColor.class, new NamedTextColorAdapter())
            .registerTypeHierarchyAdapter(ProfileProperty.class, new ProfilePropertyAdapter())
            .registerTypeHierarchyAdapter(Sound.class, new SoundAdapter())
            .registerTypeHierarchyAdapter(Title.Times.class, new TitleTimesAdapter())
            .registerTypeHierarchyAdapter(Title.class, new TitleAdapter())
            .registerTypeHierarchyAdapter(Vector3f.class, new Vector3fAdapter())
            .registerTypeHierarchyAdapter(World.class, new WorldAdapter(getServer()))
            .build();

    private final PaperActionTypeProvider actionTypeProvider = new PaperActionTypeProvider();
    private final PaperAttributeProvider attributeProvider = new PaperAttributeProvider(this);
    private final PaperCharacterController characterController = new PaperCharacterController(this);
    private final PaperSkinFactory skinFactory = new PaperSkinFactory(this);
    private final PluginMessenger messenger = new PluginMessenger(this);

    public final ActionType<Component> sendActionbar = register(new PaperActionType<>("send_actionbar", Component.class, Audience::sendActionBar));
    public final ActionType<Component> sendMessage = register(new PaperActionType<>("send_message", Component.class, Audience::sendMessage));
    public final ActionType<InetSocketAddress> transfer = register(new PaperActionType<>("transfer", InetSocketAddress.class,
            (player, address) -> player.transfer(address.getHostName(), address.getPort())));
    public final ActionType<Location> teleport = (register(new PaperActionType<>("teleport", Location.class, Entity::teleportAsync)));
    public final ActionType<Sound> playSound = register(new PaperActionType<>("play_sound", Sound.class, Audience::playSound));
    public final ActionType<String> runConsoleCommand = register(new PaperActionType<>("run_console_command", String.class,
            (player, command) -> player.getServer().dispatchCommand(player.getServer().getConsoleSender(), command)));
    public final ActionType<String> runCommand = register(new PaperActionType<>("run_command", String.class, Player::performCommand));
    public final ActionType<Title> sendTitle = register(new PaperActionType<>("send_title", Title.class, Audience::showTitle));
    public final ActionType<String> connect = register(new PaperActionType<>("connect", String.class, messenger::connect));

    private final ComponentBundle bundle = new ComponentBundle(translations,
            audience -> audience instanceof Player player ? player.locale() : Locale.US)
            .register("messages", Locale.US)
            .register("messages_german", Locale.GERMANY)
            .miniMessage(bundle -> MiniMessage.builder().tags(TagResolver.resolver(
                    TagResolver.standard(),
                    Placeholder.component("prefix", bundle.component(Locale.US, "prefix"))
            )).build());

    @Override
    public void onLoad() {
        getServer().getServicesManager().register(CharacterProvider.class, this, this, ServicePriority.Highest);
    }

    @Override
    public void onDisable() {
        characterController.getCharacters().forEach(character -> {
            character.persist();
            character.remove();
        });
        metrics.shutdown();
    }

    @Override
    public void onEnable() {
        readAll().forEach(character -> {
            var location = character.getSpawnLocation();
            if (location == null || character.spawn(location)) return;
            getComponentLogger().error("Failed to spawn character {}", character.getName());
        });
        registerCommands();
        registerListeners();
    }

    public @Unmodifiable List<Character<?>> readAll() {
        var files = savesFolder.listFiles((file, name) -> name.endsWith(".dat"));
        return files == null ? List.of() : Arrays.stream(files)
                .map(this::readSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    public Character<?> read(File file) throws IOException {
        try (var inputStream = stream(IO.of(file))) {
            return read(inputStream);
        } catch (Exception e) {
            var io = IO.of(file.getPath() + "_old");
            if (!io.exists()) throw e;
            getComponentLogger().warn("Failed to load character from {}", file.getPath(), e);
            getComponentLogger().warn("Falling back to {}", io);
            try (var inputStream = stream(io)) {
                return read(inputStream);
            }
        }
    }

    public ComponentBundle bundle() {
        return bundle;
    }

    public File savesFolder() {
        return savesFolder;
    }

    public NBT nbt() {
        return nbt;
    }

    @Override
    public PaperActionTypeProvider actionTypeProvider() {
        return actionTypeProvider;
    }

    @Override
    public PaperAttributeProvider attributeProvider() {
        return attributeProvider;
    }

    @Override
    public PaperCharacterController characterController() {
        return characterController;
    }

    @Override
    public PaperSkinFactory skinFactory() {
        return skinFactory;
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event ->
                event.registrar().register(CharacterCommand.create(this), List.of("npc"))));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CharacterListener(), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);

        getServer().getPluginManager().registerEvents(new test(this), this);
    }

    private @Nullable Character<?> readSafe(File file) {
        try {
            return read(file);
        } catch (EOFException e) {
            getComponentLogger().error("The character file {} is irrecoverably broken", file.getPath());
            return null;
        } catch (Exception e) {
            getComponentLogger().error("Failed to load character from {}", file.getPath(), e);
            getComponentLogger().error("Please look for similar issues or report this on GitHub: {}", ISSUES);
            return null;
        }
    }

    private NBTInputStream stream(IO file) throws IOException {
        return new NBTInputStream(file.inputStream(READ), StandardCharsets.UTF_8);
    }

    private Character<?> read(NBTInputStream inputStream) throws IOException {
        var entry = inputStream.readNamedTag();
        var root = entry.getKey().getAsCompound();
        var name = entry.getValue().orElseThrow(() -> new ParserException("Character misses root name"));
        var type = nbt.fromTag(root.get("type"), EntityType.class);
        var location = root.optional("location").map(tag -> nbt.fromTag(tag, Location.class)).orElse(null);
        var character = type.equals(EntityType.PLAYER)
                ? createPlayerCharacter(root, name)
                : createCharacter(root, name, type);
        character.setSpawnLocation(location);
        return character;
    }

    private PlayerCharacter createPlayerCharacter(CompoundTag root, String name) {
        var uuid = root.optional("uuid").map(tag -> nbt.fromTag(tag, UUID.class)).orElseGet(UUID::randomUUID);
        var character = characterController.createCharacter(name, uuid);
        character.deserialize(root);
        return character;
    }

    private Character<?> createCharacter(CompoundTag root, String name, EntityType type) {
        var character = characterController.createCharacter(name, type);
        character.deserialize(root);
        return character;
    }

    private <T> ActionType<T> register(ActionType<T> actionType) {
        return actionTypeProvider().register(actionType);
    }
}
