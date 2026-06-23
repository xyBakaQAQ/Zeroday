package com.xybakaqaq.utils;

import net.minecraft.client.Minecraft;

/**
 * Convenience access to the client. Implement this interface to use {@code mc} directly
 * instead of writing {@code Minecraft.getInstance()} everywhere.
 *
 * <p>It is an <em>interface</em> field on purpose: interface static fields initialize lazily on
 * first access (at runtime, when the client exists), not at class-load time during mod init
 * (when {@code Minecraft.getInstance()} would still be {@code null}). So don't read {@code mc}
 * from a constructor that runs during init.
 */
public interface IMinecraft {

	Minecraft mc = Minecraft.getInstance();
}
