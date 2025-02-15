package net.wurstclient.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.EntityTypeListEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.text.WText;

import java.util.*;

public class EntityTypeListSetting extends Setting
{
	private final ArrayList<String> entityTypeNames = new ArrayList<>();
	private final ArrayList<EntityType<?>> entityTypes = new ArrayList<>();
	private final String[] defaultTypes;
	
	public EntityTypeListSetting(String name, WText description,
		String... entities)
	{
		super(name, description);
		
		Arrays.stream(entities).parallel()
			.map(s -> Registries.ENTITY_TYPE.get(new Identifier(s)))
			.filter(Objects::nonNull).map(EntityUtils::getName).distinct()
			.sorted().forEachOrdered(entityTypeNames::add);
		
		entityTypes.addAll(Arrays.stream(entities).parallel()
			.map(s -> Registries.ENTITY_TYPE.get(new Identifier(s)))
			.filter(Objects::nonNull).toList());
		
		defaultTypes = entityTypeNames.toArray(new String[0]);
	}
	
	public EntityTypeListSetting(String name, String descriptionKey,
		String... entities)
	{
		this(name, WText.translated(descriptionKey), entities);
	}
	
	public List<String> getEntityTypeNames()
	{
		return Collections.unmodifiableList(entityTypeNames);
	}
	
	public List<EntityType<?>> getEntityTypes()
	{
		return Collections.unmodifiableList(entityTypes);
	}
	
	public int indexOf(String name)
	{
		return Collections.binarySearch(entityTypeNames, name);
	}
	
	public int size()
	{
		return entityTypeNames.size();
	}
	
	public void add(EntityType<?> entityType)
	{
		String name = EntityUtils.getName(entityType);
		if(Collections.binarySearch(entityTypeNames, name) >= 0)
			return;
		
		entityTypes.add(entityType);
		Collections.sort(entityTypes,
			Comparator.comparing(EntityUtils::getName));
		
		entityTypeNames.add(name);
		Collections.sort(entityTypeNames);
		
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(String name)
	{
		remove(indexOf(name));
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= entityTypeNames.size())
			return;
		
		entityTypes.remove(index);
		entityTypeNames.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefaults()
	{
		entityTypeNames.clear();
		entityTypes.clear();
		
		entityTypeNames.addAll(Arrays.asList(defaultTypes));
		
		// here we must s -> Registries.ENTITY_TYPE.get(new Identifier(s))
		entityTypes.addAll(Arrays.stream(defaultTypes).parallel()
			.map(s -> Registries.ENTITY_TYPE.get(new Identifier(s)))
			.filter(Objects::nonNull).toList());
		
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new EntityTypeListEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			entityTypeNames.clear();
			
			if(JsonUtils.getAsString(json, "nope").equals("default"))
			{
				entityTypeNames.addAll(Arrays.asList(defaultTypes));
				return;
			}
			
			JsonUtils.getAsArray(json).getAllStrings().parallelStream()
				.map(s -> Registries.ENTITY_TYPE.get(new Identifier(s)))
				.filter(Objects::nonNull).map(EntityUtils::getName).distinct()
				.sorted().forEachOrdered(name -> {
					EntityType<?> entityType =
						Registries.ENTITY_TYPE.get(new Identifier(name));
					entityTypes.add(entityType);
					entityTypeNames.add(name);
				});
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefaults();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		if(entityTypeNames.equals(Arrays.asList(defaultTypes)))
		{
			return new JsonPrimitive("default");
		}
		
		JsonArray json = new JsonArray();
		entityTypeNames.forEach(s -> json.add(s));
		return json;
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "EntityType");
		
		JsonArray defaultTypesJson = new JsonArray();
		for(String entityTypeName : defaultTypes)
			defaultTypesJson.add(entityTypeName);
		
		json.add("defaultTypes", defaultTypesJson);
		
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		String fullname = featureName + " " + getName();
		
		String command = ".entitytype " + featureName.toLowerCase() + " ";
		command += getName().toLowerCase().replace(" ", "_") + " ";
		
		LinkedHashSet<PossibleKeybind> pkb = new LinkedHashSet<>();
		pkb.add(new PossibleKeybind(command + "reset", "Reset " + fullname));
		
		return pkb;
	}
	
}
