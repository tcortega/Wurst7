/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.entity.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.wurstclient.Category;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.chestesp.ChestEspBlockGroup;
import net.wurstclient.hacks.chestesp.ChestEspEntityGroup;
import net.wurstclient.hacks.chestesp.ChestEspGroup;
import net.wurstclient.hacks.chestesp.ChestEspRenderer;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EspStyleSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.chunk.ChunkUtils;

public class ChestEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EspStyleSetting style = new EspStyleSetting();
	
	private final ChestEspBlockGroup basicChests = new ChestEspBlockGroup(
		new ColorSetting("Chest color",
			"Normal chests will be highlighted in this color.", Color.GREEN),
		null);
	
	private final ChestEspBlockGroup trapChests = new ChestEspBlockGroup(
		new ColorSetting("Trap chest color",
			"Trapped chests will be highlighted in this color.",
			new Color(0xFF8000)),
		new CheckboxSetting("Include trap chests", true));
	
	private final ChestEspBlockGroup enderChests = new ChestEspBlockGroup(
		new ColorSetting("Ender color",
			"Ender chests will be highlighted in this color.", Color.CYAN),
		new CheckboxSetting("Include ender chests", true));
	
	private final ChestEspEntityGroup chestCarts =
		new ChestEspEntityGroup(
			new ColorSetting("Chest cart color",
				"Minecarts with chests will be highlighted in this color.",
				Color.YELLOW),
			new CheckboxSetting("Include chest carts", true));
	
	private final ChestEspEntityGroup chestBoats =
		new ChestEspEntityGroup(
			new ColorSetting("Chest boat color",
				"Boats with chests will be highlighted in this color.",
				Color.YELLOW),
			new CheckboxSetting("Include chest boats", true));
	
	private final ChestEspBlockGroup barrels = new ChestEspBlockGroup(
		new ColorSetting("Barrel color",
			"Barrels will be highlighted in this color.", Color.GREEN),
		new CheckboxSetting("Include barrels", true));
	
	private final ChestEspBlockGroup shulkerBoxes = new ChestEspBlockGroup(
		new ColorSetting("Shulker color",
			"Shulker boxes will be highlighted in this color.", Color.MAGENTA),
		new CheckboxSetting("Include shulkers", true));
	
	private final ChestEspBlockGroup hoppers = new ChestEspBlockGroup(
		new ColorSetting("Hopper color",
			"Hoppers will be highlighted in this color.", Color.WHITE),
		new CheckboxSetting("Include hoppers", false));
	
	private final ChestEspEntityGroup hopperCarts =
		new ChestEspEntityGroup(
			new ColorSetting("Hopper cart color",
				"Minecarts with hoppers will be highlighted in this color.",
				Color.YELLOW),
			new CheckboxSetting("Include hopper carts", false));
	
	private final ChestEspBlockGroup droppers = new ChestEspBlockGroup(
		new ColorSetting("Dropper color",
			"Droppers will be highlighted in this color.", Color.WHITE),
		new CheckboxSetting("Include droppers", false));
	
	private final ChestEspBlockGroup dispensers = new ChestEspBlockGroup(
		new ColorSetting("Dispenser color",
			"Dispensers will be highlighted in this color.",
			new Color(0xFF8000)),
		new CheckboxSetting("Include dispensers", false));
	
	private final ChestEspBlockGroup furnaces =
		new ChestEspBlockGroup(new ColorSetting("Furnace color",
			"Furnaces, smokers, and blast furnaces will be highlighted in this color.",
			Color.RED), new CheckboxSetting("Include furnaces", false));
	
	private final List<ChestEspGroup> groups = Arrays.asList(basicChests,
		trapChests, enderChests, chestCarts, chestBoats, barrels, shulkerBoxes,
		hoppers, hopperCarts, droppers, dispensers, furnaces);
	
	private final List<ChestEspEntityGroup> entityGroups =
		Arrays.asList(chestCarts, chestBoats, hopperCarts);
	
	private static Class<?> IHasOpenersClass;
	private static MethodHandle getOpenersHandle;
	private static boolean lootrReflectionAttempted = false;
	private static boolean lootrPresent = false;
	
	private final Map<Object, Boolean> openedContainerCache =
		new ConcurrentHashMap<>();
	private long lastCacheClear = System.currentTimeMillis();
	private static final long CACHE_CLEAR_INTERVAL = 5000;
	
	private UUID playerUuid;
	
	public ChestEspHack()
	{
		super("ChestESP");
		setCategory(Category.RENDER);
		
		addSetting(style);
		groups.stream().flatMap(ChestEspGroup::getSettings)
			.forEach(this::addSetting);
	}
	
	private void initializeLootrReflection()
	{
		if(lootrReflectionAttempted)
		{
			return;
		}
		
		try
		{
			IHasOpenersClass =
				Class.forName("net.zestyblaze.lootr.api.IHasOpeners");
			
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			MethodType methodType = MethodType.methodType(Set.class);
			getOpenersHandle =
				lookup.findVirtual(IHasOpenersClass, "getOpeners", methodType);
			
			lootrPresent = true;
			System.out.println(
				"Wurst Client: Lootr compatibility enabled (using MethodHandles).");
			
		}catch(Exception e)
		{
			IHasOpenersClass = null;
			getOpenersHandle = null;
			lootrPresent = false;
			System.out.println(
				"Wurst Client: Lootr not found, compatibility disabled.");
		}
		
		lootrReflectionAttempted = true;
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		initializeLootrReflection();
		
		if(MC.player != null)
		{
			playerUuid = MC.player.getUuid();
		}
		
		ChestEspRenderer.prepareBuffers();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		groups.forEach(ChestEspGroup::clear);
		openedContainerCache.clear();
		ChestEspRenderer.closeBuffers();
	}
	
	private boolean isLootrContainerOpened(Object container)
	{
		if(!lootrPresent)
		{
			return false;
		}
		
		Boolean cached = openedContainerCache.get(container);
		if(cached != null)
		{
			return cached;
		}
		
		if(container == null || !IHasOpenersClass.isInstance(container))
		{
			openedContainerCache.put(container, false);
			return false;
		}
		
		try
		{
			@SuppressWarnings("unchecked")
			Set<UUID> openers = (Set<UUID>)getOpenersHandle.invoke(container);
			
			boolean isOpened = openers != null && playerUuid != null
				&& openers.contains(playerUuid);
			
			openedContainerCache.put(container, isOpened);
			return isOpened;
			
		}catch(Throwable e)
		{
			System.err.println(
				"Wurst Client: Error invoking Lootr method. Disabling compatibility.");
			getOpenersHandle = null;
			lootrPresent = false;
			openedContainerCache.put(container, false);
			return false;
		}
	}
	
	private void clearCacheIfNeeded()
	{
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastCacheClear > CACHE_CLEAR_INTERVAL)
		{
			openedContainerCache.clear();
			lastCacheClear = currentTime;
		}
	}
	
	@Override
	public void onUpdate()
	{
		groups.forEach(ChestEspGroup::clear);
		
		clearCacheIfNeeded();
		
		if(MC.player != null && playerUuid == null)
		{
			playerUuid = MC.player.getUuid();
		}
		
		ArrayList<BlockEntity> blockEntities =
			ChunkUtils.getLoadedBlockEntities()
				.collect(Collectors.toCollection(ArrayList::new));
		
		if(lootrPresent)
		{
			List<BlockEntity> nonLootrEntities = new ArrayList<>();
			for(BlockEntity blockEntity : blockEntities)
			{
				if(!isLootrContainerOpened(blockEntity))
				{
					nonLootrEntities.add(blockEntity);
				}
			}
			processBlockEntities(nonLootrEntities);
		}else
		{
			processBlockEntities(blockEntities);
		}
		
		processEntities();
	}
	
	private void processBlockEntities(List<BlockEntity> blockEntities)
	{
		for(BlockEntity blockEntity : blockEntities)
		{
			if(blockEntity instanceof ChestBlockEntity)
			{
				if(blockEntity instanceof TrappedChestBlockEntity)
				{
					trapChests.add(blockEntity);
				}else
				{
					basicChests.add(blockEntity);
				}
			}else if(blockEntity instanceof BarrelBlockEntity)
			{
				barrels.add(blockEntity);
			}else if(blockEntity instanceof ShulkerBoxBlockEntity)
			{
				shulkerBoxes.add(blockEntity);
			}else if(blockEntity instanceof EnderChestBlockEntity)
			{
				enderChests.add(blockEntity);
			}else if(blockEntity instanceof AbstractFurnaceBlockEntity)
			{
				furnaces.add(blockEntity);
			}else if(blockEntity instanceof HopperBlockEntity)
			{
				hoppers.add(blockEntity);
			}else if(blockEntity instanceof DispenserBlockEntity)
			{
				dispensers.add(blockEntity);
			}else if(blockEntity instanceof DropperBlockEntity)
			{
				droppers.add(blockEntity);
			}
		}
	}
	
	private void processEntities()
	{
		for(Entity entity : MC.world.getEntities())
		{
			if(lootrPresent && isLootrContainerOpened(entity))
			{
				continue;
			}
			
			if(entity instanceof ChestMinecartEntity)
			{
				chestCarts.add(entity);
			}else if(entity instanceof ChestBoatEntity)
			{
				chestBoats.add(entity);
			}else if(entity instanceof HopperMinecartEntity)
			{
				hopperCarts.add(entity);
			}
		}
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.hasLines())
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		entityGroups.stream().filter(ChestEspGroup::isEnabled)
			.forEach(g -> g.updateBoxes(partialTicks));
		
		ChestEspRenderer espRenderer =
			new ChestEspRenderer(matrixStack, partialTicks);
		
		if(style.hasBoxes())
		{
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			groups.stream().filter(ChestEspGroup::isEnabled)
				.forEach(espRenderer::renderBoxes);
		}
		
		if(style.hasLines())
		{
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			groups.stream().filter(ChestEspGroup::isEnabled)
				.forEach(espRenderer::renderLines);
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
