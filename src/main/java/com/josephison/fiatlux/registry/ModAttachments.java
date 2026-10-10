package com.josephison.fiatlux.registry;

import com.josephison.fiatlux.FiatLux;
import com.josephison.fiatlux.divinity.Alignment;
import com.josephison.fiatlux.divinity.AngelState;
import com.josephison.fiatlux.divinity.DivineState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Per-entity data attachments. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FiatLux.MODID);

    public static final Supplier<AttachmentType<DivineState>> DIVINE_STATE = ATTACHMENTS.register("divine_state",
            () -> AttachmentType.builder(() -> DivineState.DEFAULT).serialize(DivineState.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<Alignment>> ALIGNMENT = ATTACHMENTS.register("alignment",
            () -> AttachmentType.builder(() -> Alignment.NEUTRAL).serialize(Alignment.CODEC).copyOnDeath().build());

    /** Pride and loyalty of one of the Host (the Angelic Rebellion). */
    public static final Supplier<AttachmentType<AngelState>> ANGEL_STATE = ATTACHMENTS.register("angel_state",
            () -> AttachmentType.builder(() -> AngelState.UNSET).serialize(AngelState.CODEC).build());

    private ModAttachments() {}
}
