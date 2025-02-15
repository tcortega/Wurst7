package net.wurstclient.commands;

import net.minecraft.entity.EntityType;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.MobEspHack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EntityUtils;

import java.util.List;

public class MobEspCmd extends Command
{
	public MobEspCmd()
	{
		super("mobesp", "Manages the MobESP feature", ".mobesp add <entity>",
			".mobesp remove <entity>", ".mobesp list", ".mobesp reset");
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
		List<String> names = getHack().getEntitySettings().getEntityTypeNames();
		if(names.isEmpty())
		{
			ChatUtils.message("No entities are being tracked.");
		}else
		{
			ChatUtils.message("Tracked entities: " + String.join(", ", names));
		}
	}
	
	private void reset()
	{
		getHack().getEntitySettings().resetToDefaults();
		ChatUtils.message("MobESP entity list has been reset.");
	}
	
	private void add(String entityName)
	{
		EntityType<?> entity = EntityUtils.getTypeFromName(entityName);
		if(entity == null)
		{
			ChatUtils.error("Invalid entity name: " + entityName);
			return;
		}
		
		getHack().getEntitySettings().add(entity);
		ChatUtils.message("Added entity to MobESP: " + entityName);
	}
	
	private void remove(String entity)
	{
		getHack().getEntitySettings().remove(entity);
		ChatUtils.message("Removed entity from MobESP: " + entity);
	}
	
	private MobEspHack getHack()
	{
		return WURST.getHax().mobEspHack;
	}
}
