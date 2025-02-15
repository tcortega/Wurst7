package net.wurstclient.clickgui.components;

import java.util.Objects;

import net.wurstclient.clickgui.screens.EditEntityTypeListScreen;
import net.wurstclient.settings.EntityTypeListSetting;
import net.wurstclient.settings.Setting;

public final class EntityTypeListEditButton extends AbstractListEditButton
{
	private final EntityTypeListSetting setting;
	
	public EntityTypeListEditButton(EntityTypeListSetting setting)
	{
		this.setting = Objects.requireNonNull(setting);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	protected void openScreen()
	{
		MC.setScreen(new EditEntityTypeListScreen(MC.currentScreen, setting));
	}
	
	@Override
	protected String getText()
	{
		return setting.getName() + ": " + setting.size();
	}
	
	@Override
	protected Setting getSetting()
	{
		return setting;
	}
}
