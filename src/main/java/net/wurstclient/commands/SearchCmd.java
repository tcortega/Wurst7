package net.wurstclient.commands;

import net.minecraft.block.Block;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.SearchHack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;

import java.util.List;

public class SearchCmd extends Command
{
	public SearchCmd()
	{
		super("search", "Manages the Search feature", ".search add <entity>",
			".search remove <entity>", ".search list", ".search reset");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
		{
			throw new CmdSyntaxError();
		}
		
		switch(args[0].toLowerCase())
		{
			case "list":
			list();
			break;
			case "reset":
			reset();
			break;
			case "add":
			if(args.length < 2)
				throw new CmdSyntaxError();
			add(args[1]);
			break;
			case "remove":
			if(args.length < 2)
				throw new CmdSyntaxError();
			remove(args[1]);
			break;
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void list()
	{
		List<String> names = getHack().getBlockListSetting().getBlockNames();
		if(names.isEmpty())
		{
			ChatUtils.message("No blocks are being tracked.");
		}else
		{
			ChatUtils.message("Tracked blocks: " + String.join(", ", names));
		}
	}
	
	private void reset()
	{
		getHack().getBlockListSetting().resetToDefaults();
		ChatUtils.message("Search block list has been reset.");
	}
	
	private void add(String blockName)
	{
		Block block = BlockUtils.getBlockFromNameOrID(blockName);
		if(block == null)
		{
			ChatUtils.error("Invalid block name: " + blockName);
			return;
		}
		
		getHack().getBlockListSetting().add(block);
		ChatUtils.message("Added block to Search: " + blockName);
	}
	
	private void remove(String block)
	{
		getHack().getBlockListSetting().remove(block);
		ChatUtils.message("Removed block from Search: " + block);
	}
	
	private SearchHack getHack()
	{
		return WURST.getHax().searchHack;
	}
}
