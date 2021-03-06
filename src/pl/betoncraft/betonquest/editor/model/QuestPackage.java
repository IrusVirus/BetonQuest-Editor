/**
 * BetonQuest Editor - advanced quest creating tool for BetonQuest
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.editor.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pl.betoncraft.betonquest.editor.BetonQuestEditor;
import pl.betoncraft.betonquest.editor.controller.ConversationController;
import pl.betoncraft.betonquest.editor.controller.ExceptionController;
import pl.betoncraft.betonquest.editor.controller.NameEditController;
import pl.betoncraft.betonquest.editor.controller.RootController;
import pl.betoncraft.betonquest.editor.data.ConditionWrapper;
import pl.betoncraft.betonquest.editor.data.Editable;
import pl.betoncraft.betonquest.editor.data.ID;
import pl.betoncraft.betonquest.editor.data.IdWrapper;
import pl.betoncraft.betonquest.editor.data.TranslatableText;
import pl.betoncraft.betonquest.editor.model.exception.PackageNotFoundException;

/**
 * Keeps all data about the quest package.
 *
 * @author Jakub Sapalski
 */
public class QuestPackage implements Editable {

	private final StringProperty packName;
	private String defLang;
	private final HashMap<String, Integer> languages = new HashMap<>();
	private final ObservableList<Conversation> conversations = FXCollections.observableArrayList();
	private final ObservableList<Event> events = FXCollections.observableArrayList();
	private final ObservableList<Condition> conditions = FXCollections.observableArrayList();
	private final ObservableList<Objective> objectives = FXCollections.observableArrayList();
	private final ObservableList<JournalEntry> journal = FXCollections.observableArrayList();
	private final ObservableList<Item> items = FXCollections.observableArrayList();
	private final ObservableList<GlobalVariable> variables = FXCollections.observableArrayList();
	private final ObservableList<GlobalLocation> locations = FXCollections.observableArrayList();
	private final ObservableList<StaticEvent> staticEvents = FXCollections.observableArrayList();
	private final ObservableList<QuestCanceler> cancelers = FXCollections.observableArrayList();
	private final ObservableList<NpcBinding> npcBindings = FXCollections.observableArrayList();
	private final ObservableList<MainPageLine> mainPage = FXCollections.observableArrayList();
	private final ObservableList<Tag> tags = FXCollections.observableArrayList();
	private final ObservableList<PointCategory> points = FXCollections.observableArrayList();

	/**
	 * Loads a package using a hashmap containing all data. The key is a file
	 * name, the value is a hashmap containing keys and values of that YAML
	 * file.
	 * 
	 * @param map
	 */
	private QuestPackage(String id, HashMap<String, LinkedHashMap<String, String>> data) {
		packName = new SimpleStringProperty(id);
		try {
			// handling journal.yml
			HashMap<String, String> journalMap = data.get("journal");
			int journalIndex = 0;
			for (Entry<String, String> entry : journalMap.entrySet()) {
				String value = entry.getValue();
				String[] parts = entry.getKey().split("\\.");
				// getting the right entry
				JournalEntry journalEntry = newByID(parts[0], name -> new JournalEntry(this, name));
				if (journalEntry.getIndex() < 0) journalEntry.setIndex(journalIndex++);
				// handling entry data
				if (parts.length > 1) {
					String lang = parts[1];
					if (!languages.containsKey(lang)) {
						languages.put(lang, 1);
					} else {
						languages.put(lang, languages.get(lang) + 1);
					}
					journalEntry.getText().addLang(lang, value);
				} else {
					journalEntry.getText().setDef(value);
				}
			}
			// handling items.yml
			HashMap<String, String> itemsMap = data.get("items");
			int itemIndex = 0;
			for (String key : itemsMap.keySet()) {
				Item item = newByID(key, name -> new Item(this, name));
				item.getInstruction().set(itemsMap.get(key));
				if (item.getIndex() < 0) item.setIndex(itemIndex++);
			}
			// handling conditions.yml
			HashMap<String, String> conditionsMap = data.get("conditions");
			int conditionIndex = 0;
			for (String key : conditionsMap.keySet()) {
				Condition condition = newByID(key, name -> new Condition(this, name));
				condition.getInstruction().set(conditionsMap.get(key));
				if (condition.getIndex() < 0) condition.setIndex(conditionIndex++);
			}
			// handling event.yml
			HashMap<String, String> eventsMap = data.get("events");
			int eventIndex = 0;
			for (String key : eventsMap.keySet()) {
				Event event = newByID(key, name -> new Event(this, name));
				event.getInstruction().set(eventsMap.get(key));
				if (event.getIndex() < 0) event.setIndex(eventIndex++);
			}
			// handling objectives.yml
			HashMap<String, String> objectivesMap = data.get("objectives");
			int objectiveIndex = 0;
			for (String key : objectivesMap.keySet()) {
				Objective objective = newByID(key, name -> new Objective(this, name));
				objective.getInstruction().set(objectivesMap.get(key));
				if (objective.getIndex() < 0) objective.setIndex(objectiveIndex++);
			}
			// handling conversations/
			int convIndex = 0;
			for (Entry<String, LinkedHashMap<String, String>> entry : data.entrySet()) {
				String key = entry.getKey();
				HashMap<String, String> value = entry.getValue();
				if (key.startsWith("conversations.")) {
					HashMap<String, String> convData = value;
					String convName = key.substring(14);
					Conversation conv = newByID(convName, name -> new Conversation(this, name));
					if (conv.getIndex() < 0) conv.setIndex(convIndex++);
					int playerIndex = 0;
					int npcIndex = 0;
					// handling conversation.yml
					for (Entry<String, String> subEntry : convData.entrySet()) {
						String subKey = subEntry.getKey();
						String subValue = subEntry.getValue();
						// reading NPC name, optionally in multiple languages
						if (subKey.equals("quester")) {
							conv.getNPC().setDef(subValue);
						} else if (subKey.startsWith("quester.")) {
							String lang = subKey.substring(8);
							if (!languages.containsKey(lang)) {
								languages.put(lang, 1);
							} else {
								languages.put(lang, languages.get(lang) + 1);
							}
							conv.getNPC().addLang(lang, subValue);
						}
						// reading the stop option
						else if (subKey.equals("stop")) {
							conv.getStop().set(subValue.equalsIgnoreCase("true"));
						}
						// reading starting options
						else if (subKey.equals("first")) {
							String[] pointerNames = subValue.split(",");
							ArrayList<IdWrapper<NpcOption>> options = new ArrayList<>(pointerNames.length);
							for (int i = 0; i < pointerNames.length; i++) {
								IdWrapper<NpcOption> startingOption = new IdWrapper<>(this, conv.newNpcOption(pointerNames[i].trim()));
								options.add(i, startingOption);
								startingOption.setIndex(i); 
							}
							conv.getStartingOptions().addAll(options);
						}
						// reading final events
						else if (subKey.equals("final")) {
							String[] eventNames = subValue.split(",");
							ArrayList<IdWrapper<Event>> events = new ArrayList<>(eventNames.length);
							for (int i = 0; i < eventNames.length; i++) {
								IdWrapper<Event> finalEvent = new IdWrapper<>(this, newByID(eventNames[i].trim(), name -> new Event(this, name)));
								events.add(i, finalEvent);
								finalEvent.setIndex(i);
							}
							conv.getFinalEvents().addAll(events);
						}
						// reading NPC options
						else if (subKey.startsWith("NPC_options.")) {
							String[] parts = subKey.split("\\.");
							if (parts.length > 1) {
								String optionName = parts[1];
								// resolving an option
								NpcOption option = conv.newNpcOption(optionName);
								if (option.getIndex() < 0) option.setIndex(npcIndex++);
								if (parts.length > 2) {
									// getting specific values
									switch (parts[2]) {
									case "text":
										if (parts.length > 3) {
											String lang = parts[3];
											if (!languages.containsKey(lang)) {
												languages.put(lang, 1);
											} else {
												languages.put(lang, languages.get(lang) + 1);
											}
											option.getText().addLang(lang, subValue);
										} else {
											option.getText().setDef(subValue);
										}
										break;
									case "event":
									case "events":
										String[] eventNames = subValue.split(",");
										ArrayList<IdWrapper<Event>> events = new ArrayList<>(eventNames.length);
										for (int i = 0; i < eventNames.length; i++) {
											IdWrapper<Event> event = new IdWrapper<>(this, newByID(eventNames[i].trim(), name -> new Event(this, name)));
											events.add(i, event);
											event.setIndex(i);
										}
										option.getEvents().addAll(events);
										break;
									case "condition":
									case "conditions":
										String[] conditionNames = subValue.split(",");
										ArrayList<ConditionWrapper> conditions = new ArrayList<>(conditionNames.length);
										for (int i = 0; i < conditionNames.length; i++) {
											String name = conditionNames[i].trim();
											boolean negated = false;
											while (name.startsWith("!")) {
												name = name.substring(1, name.length());
												negated = true;
											}
											ConditionWrapper condition = new ConditionWrapper(this, newByID(name, idString -> new Condition(this, idString)));
											condition.setNegated(negated);
											conditions.add(i, condition);
											condition.setIndex(i);
										}
										option.getConditions().addAll(conditions);
										break;
									case "pointer":
									case "pointers":
										String[] pointerNames = subValue.split(",");
										ArrayList<IdWrapper<ConversationOption>> options = new ArrayList<>(pointerNames.length);
										for (int i = 0; i < pointerNames.length; i++) {
											IdWrapper<ConversationOption> pointer = new IdWrapper<>(this, conv.newPlayerOption(pointerNames[i].trim()));
											options.add(i, pointer);
											pointer.setIndex(i);
										}
										option.getPointers().addAll(options);
										break;
									}
								}
							}
						}
						// reading player options
						else if (subKey.startsWith("player_options.")) {
							String[] parts = subKey.split("\\.");
							if (parts.length > 1) {
								String optionName = parts[1];
								// resolving an option
								PlayerOption option = conv.newPlayerOption(optionName);
								if (option.getIndex() < 0) {
									option.setIndex(playerIndex++);
								}
								if (parts.length > 2) {
									// getting specific values
									switch (parts[2]) {
									case "text":
										if (parts.length > 3) {
											String lang = parts[3];
											if (!languages.containsKey(lang)) {
												languages.put(lang, 1);
											} else {
												languages.put(lang, languages.get(lang) + 1);
											}
											option.getText().addLang(lang, subValue);
										} else {
											option.getText().setDef(subValue);
										}
										break;
									case "event":
									case "events":
										String[] eventNames = subValue.split(",");
										ArrayList<IdWrapper<Event>> events = new ArrayList<>(eventNames.length);
										for (int i = 0; i < eventNames.length; i++) {
											IdWrapper<Event> event = new IdWrapper<>(this, newByID(eventNames[i].trim(), name -> new Event(this, name)));
											events.add(i, event);
											event.setIndex(i);
										}
										option.getEvents().addAll(events);
										break;
									case "condition":
									case "conditions":
										String[] conditionNames = subValue.split(",");
										ArrayList<ConditionWrapper> conditions = new ArrayList<>(conditionNames.length);
										for (int i = 0; i < conditionNames.length; i++) {
											String name = conditionNames[i].trim();
											boolean negated = false;
											while (name.startsWith("!")) {
												name = name.substring(1, name.length());
												negated = true;
											}
											ConditionWrapper condition = new ConditionWrapper(this, newByID(name, idString -> new Condition(this, idString)));
											condition.setNegated(negated);
											conditions.add(i, condition);
											condition.setIndex(i);
										}
										option.getConditions().addAll(conditions);
										break;
									case "pointer":
									case "pointers":
										String[] pointerNames = subValue.split(",");
										ArrayList<IdWrapper<ConversationOption>> options = new ArrayList<>(pointerNames.length);
										for (int i = 0; i < pointerNames.length; i++) {
											IdWrapper<ConversationOption> pointer = new IdWrapper<>(this, conv.newNpcOption(pointerNames[i].trim()));
											options.add(i, pointer);
											pointer.setIndex(i);
										}
										option.getPointers().addAll(options);
										break;
									}
								}
							}
						}
					}
				}
			}
			// handling main.yml
			LinkedHashMap<String, String> config = data.get("main");
			for (Entry<String, String> entry : config.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				// handling variables
				if (key.startsWith("variables.")) {
					variables.add(new GlobalVariable(this, key.substring(10), value));
				}
				// handling global locations
				else if (key.startsWith("global_locations")) {
					for (String globLoc : value.split(",")) {
						locations.add(new GlobalLocation(newByID(globLoc, name -> new Objective(this, name))));
					}
				}
				// handling static events
				else if (key.startsWith("static_events.")) {
					StaticEvent staticEvent = new StaticEvent(this, key.substring(14));
					staticEvent.getEvent().set(newByID(value, name -> new Event(this, name)));
					staticEvents.add(staticEvent);
				}
				// handling NPC-conversation bindings
				else if (key.startsWith("npcs.")) {
					npcBindings.add(new NpcBinding(this, key.substring(5), newByID(value, name -> new Conversation(this, name))));
				}
				// handling quest cancelers
				else if (key.startsWith("cancel.")) {
					String[] parts = key.split("\\.");
					if (parts.length > 1) {
						// getting the right canceler or creating new one
						QuestCanceler canceler = newByID(parts[1], name -> new QuestCanceler(this, name));
						// handling canceler properties
						if (parts.length > 2) {
							switch (parts[2]) {
							case "name":
								if (parts.length > 3) {
									String lang = parts[3];
									if (!languages.containsKey(lang)) {
										languages.put(lang, 1);
									} else {
										languages.put(lang, languages.get(lang) + 1);
									}
									canceler.getName().addLang(lang, value);
								} else {
									canceler.getName().setDef(value);
								}
								break;
							case "events":
								String[] eventNames = value.split(",");
								ArrayList<IdWrapper<Event>> events = new ArrayList<>(eventNames.length);
								for (int i = 0; i < eventNames.length; i++) {
									IdWrapper<Event> event = new IdWrapper<>(this, newByID(eventNames[i].trim(), name -> new Event(this, name)));
									events.add(i, event);
									event.setIndex(i);
								}
								canceler.getEvents().addAll(events);
								break;
							case "conditions":
								String[] conditionNames = value.split(",");
								ArrayList<ConditionWrapper> conditions = new ArrayList<>(conditionNames.length);
								for (int i = 0; i < conditionNames.length; i++) {
									String name = conditionNames[i].trim();
									boolean negated = false;
									while (name.startsWith("!")) {
										name = name.substring(1, name.length());
										negated = true;
									}
									ConditionWrapper condition = new ConditionWrapper(this, newByID(name, idString -> new Condition(this, idString)));
									condition.setNegated(negated);
									conditions.add(i, condition);
									condition.setIndex(i);
								}
								canceler.getConditions().addAll(conditions);
								break;
							case "objectives":
								String[] objectiveNames = value.split(",");
								ArrayList<IdWrapper<Objective>> objectives = new ArrayList<>(objectiveNames.length);
								for (int i = 0; i < objectiveNames.length; i++) {
									IdWrapper<Objective> wrapper = new IdWrapper<>(this, newByID(objectiveNames[i].trim(), name -> new Objective(this, name)));
									objectives.add(i, wrapper);
									wrapper.setIndex(i);
								}
								canceler.getObjectives().addAll(objectives);
								break;
							case "tags":
								String[] tagNames = value.split(",");
								ArrayList<IdWrapper<Tag>> tags = new ArrayList<>(tagNames.length);
								for (int i = 0; i < tagNames.length; i++) {
									IdWrapper<Tag> wrapper = new IdWrapper<>(this, newByID(tagNames[i].trim(), name -> new Tag(this, name)));
									tags.add(i, wrapper);
									wrapper.setIndex(i);
								}
								canceler.getTags().addAll(tags);
								break;
							case "points":
								String[] pointNames = value.split(",");
								ArrayList<IdWrapper<PointCategory>> points = new ArrayList<>(pointNames.length);
								for (int i = 0; i < pointNames.length; i++) {
									IdWrapper<PointCategory> wrapper = new IdWrapper<>(this, newByID(pointNames[i].trim(), name -> new PointCategory(this, name)));
									points.add(i, wrapper);
									wrapper.setIndex(i);
								}
								canceler.getPoints().addAll(points);
								break;
							case "journal":
								String[] journalNames = value.split(",");
								ArrayList<IdWrapper<JournalEntry>> journal = new ArrayList<>(journalNames.length);
								for (int i = 0; i < journalNames.length; i++) {
									IdWrapper<JournalEntry> wrapper = new IdWrapper<>(this, newByID(journalNames[i].trim(), name -> new JournalEntry(this, name)));
									journal.add(i, wrapper);
									wrapper.setIndex(i);
								}
								canceler.getJournal().addAll(journal);
								break;
							case "loc":
								canceler.setLocation(value);
								break;
							}
						}
					}
				}
				// handling journal main page
				else if (key.startsWith("journal_main_page")) {
					String[] parts = key.split("\\.");
					if (parts.length > 1) {
						MainPageLine line = newByID(parts[1], name -> new MainPageLine(this, name));
						if (parts.length > 2) {
							switch (parts[2]) {
							case "text":
								if (parts.length > 3) {
									String lang = parts[3];
									if (!languages.containsKey(lang)) {
										languages.put(lang, 1);
									} else {
										languages.put(lang, languages.get(lang) + 1);
									}
									line.getText().addLang(lang, value);
								} else {
									line.getText().setDef(value);
								}
								break;
							case "priority":
								try {
									line.getPriority().set(Integer.parseInt(value));
								} catch (NumberFormatException e) {
									// TODO error, need a number
								}
								break;
							case "conditions":
								String[] conditionNames = value.split(",");
								ArrayList<ConditionWrapper> conditions = new ArrayList<>(conditionNames.length);
								for (int i = 0; i < conditionNames.length; i++) {
									String name = conditionNames[i].trim();
									boolean negated = false;
									while (name.startsWith("!")) {
										name = name.substring(1, name.length());
										negated = true;
									}
									ConditionWrapper condition = new ConditionWrapper(this, newByID(name, idString -> new Condition(this, idString)));
									condition.setNegated(negated);
									conditions.add(i, condition);
									condition.setIndex(i);
								}
								line.getConditions().addAll(conditions);
								break;
							}
						}
					}
				}
				// handling default language
				else if (key.equalsIgnoreCase("default_language")) {
					defLang = value;
				}
			}
			// check which language is used most widely and set it as default
			if (defLang == null) {
				int max = 0;
				String maxLang = null;
				for (Entry<String, Integer> entry : languages.entrySet()) {
					if (entry.getValue() > max) {
						max = entry.getValue();
						maxLang = entry.getKey();
					}
				}
				defLang = maxLang;
			}
			// add package to a list of loaded packages
			BetonQuestEditor.getInstance().getPackages().put(packName.get(), this);
			RootController.setPackages(BetonQuestEditor.getInstance().getPackages().values());
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}

	@Override
	public boolean edit() {
		return NameEditController.display(packName);
	}

	public StringProperty getName() {
		return packName;
	}

	public String getDefLang() {
		return defLang;
	}

	public void setDefLang(String defLang) {
		this.defLang = defLang;
	}

	public ObservableList<Conversation> getConversations() {
		return conversations;
	}

	public ObservableList<Event> getEvents() {
		return events;
	}

	public ObservableList<Condition> getConditions() {
		return conditions;
	}

	public ObservableList<Objective> getObjectives() {
		return objectives;
	}

	public ObservableList<JournalEntry> getJournal() {
		return journal;
	}

	public ObservableList<Item> getItems() {
		return items;
	}

	public ObservableList<GlobalVariable> getVariables() {
		return variables;
	}

	public ObservableList<GlobalLocation> getLocations() {
		return locations;
	}

	public ObservableList<StaticEvent> getStaticEvents() {
		return staticEvents;
	}

	public ObservableList<QuestCanceler> getCancelers() {
		return cancelers;
	}

	public ObservableList<NpcBinding> getNpcBindings() {
		return npcBindings;
	}

	public ObservableList<MainPageLine> getMainPage() {
		return mainPage;
	}
	
	public ObservableList<Tag> getTags() {
		return tags;
	}
	
	public ObservableList<PointCategory> getPoints() {
		return points;
	}
	
	/**
	 * @return all NpcOptions from loaded conversations in this package, the ones from current conversation first
	 */
	public ObservableList<NpcOption> getAllNpcOptions() {
		ObservableList<NpcOption> list = FXCollections.observableArrayList();
		list.addAll(ConversationController.getDisplayedConversation().getNpcOptions());
		conversations.forEach(conv -> {
			if (!conv.equals(ConversationController.getDisplayedConversation())) {
				list.addAll(conv.getNpcOptions());
			}
		});
		return list;
	}
	
	/**
	 * @return all PlayerOptions from loaded conversations in this package, the ones from current conversation first
	 */
	public ObservableList<PlayerOption> getAllPlayerOptions() {
		ObservableList<PlayerOption> list = FXCollections.observableArrayList();
		list.addAll(ConversationController.getDisplayedConversation().getPlayerOptions());
		conversations.forEach(conv -> {
			if (!conv.equals(ConversationController.getDisplayedConversation())) {
				list.addAll(conv.getPlayerOptions());
			}
		});
		return list;
	}

	public void sort() {
		// sort lists
		ArrayList<ObservableList<? extends ID>> lists = new ArrayList<>();
		lists.add(conversations);
		lists.add(events);
		lists.add(conditions);
		lists.add(objectives);
		lists.add(items);
		lists.add(journal);
		lists.add(staticEvents);
		lists.add(variables);
		lists.add(npcBindings);
		lists.add(mainPage);
		for (Conversation conv : conversations) {
			lists.add(conv.getNpcOptions());
			lists.add(conv.getPlayerOptions());
			lists.add(conv.getStartingOptions());
			lists.add(conv.getFinalEvents());
			ArrayList<ConversationOption> list = new ArrayList<>(conv.getNpcOptions());
			list.addAll(conv.getPlayerOptions());
			for (ConversationOption option : list) {
				lists.add(option.getConditions());
				lists.add(option.getEvents());
				lists.add(option.getPointers());
			}
		}
		for (ObservableList<? extends ID> list : lists) {
			list.sort((ID o1, ID o2) -> o1.getIndex() - o2.getIndex());
			int index = 0;
			for (ID object : list) {
				object.setIndex(index++);
			}
		}
	}

	@Override
	public String toString() {
		return packName.get();
	}
	
	public <T extends ID> T newByID(String id, Generator<T> generator) {
		T object = generator.generate(id);
		ObservableList<T> list = object.getList();
		T existing = null;
		for (T check : list) {
			if (check.getId().get().equals(id)) {
				existing = check;
				break;
			}
		}
		if (existing == null) {
			existing = object;
			list.add(existing);
		}
		return existing;
	}
	
	public interface Generator<T> {
		public T generate(String id);
	}

	public static QuestPackage loadFromZip(ZipFile file) throws IOException, PackageNotFoundException {
		HashMap<String, ZipEntry> zipEntries = new HashMap<>();
		Enumeration<? extends ZipEntry> entries = file.entries();
		String packName = null;
		// extract correct entries from the zip file
		while (true) {
			try {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				// get the correct path separator (both can be used)
				int index = entryName.indexOf('/');
				char separator = '/';
				if (index < 0) {
					index = entryName.indexOf('\\');
					separator = '\\';
				}
				if (index < 0) {
					continue;
				}
				packName = entryName.substring(0, entryName.indexOf(separator));
				if (!entryName.endsWith(".yml"))
					continue;
				if (entryName.contains("conversations" + separator)) {
					String convName = entryName.substring(entryName.lastIndexOf(separator) + 1, entryName.length() - 4);
					zipEntries.put("conversations." + convName, entry);
				} else {
					if (entryName.endsWith("main.yml")) {
						zipEntries.put("main", entry);
					} else if (entryName.endsWith("events.yml")) {
						zipEntries.put("events", entry);
					} else if (entryName.endsWith("conditions.yml")) {
						zipEntries.put("conditions", entry);
					} else if (entryName.endsWith("objectives.yml")) {
						zipEntries.put("objectives", entry);
					} else if (entryName.endsWith("journal.yml")) {
						zipEntries.put("journal", entry);
					} else if (entryName.endsWith("items.yml")) {
						zipEntries.put("items", entry);
					}
				}
			} catch (NoSuchElementException e) {
				break;
			}
		}
		// check if everything is loaded
		if (!zipEntries.containsKey("main") || !zipEntries.containsKey("events")
				|| !zipEntries.containsKey("conditions") || !zipEntries.containsKey("objectives")
				|| !zipEntries.containsKey("journal") || !zipEntries.containsKey("items")) {
			file.close();
			throw new PackageNotFoundException("Package does not contain required files");
		}
		// parse the yaml into hashmaps
		HashMap<String, LinkedHashMap<String, String>> values = new LinkedHashMap<>();
		for (String name : zipEntries.keySet()) {
			values.put(name, new LinkedHashMap<>());
			YAMLParser parser = new YAMLFactory().createParser(file.getInputStream(zipEntries.get(name)));
			String currentPath = "";
			String fieldName = "";
			while (true) {
				JsonToken token = parser.nextToken();
				if (token == null)
					break;
				switch (token) {
				case START_OBJECT:
					currentPath = currentPath + fieldName + ".";
					break;
				case FIELD_NAME:
					fieldName = parser.getText();
					break;
				case END_OBJECT:
					currentPath = currentPath.substring(0, currentPath.substring(0, currentPath.length() - 1).lastIndexOf(".") + 1);
					break;
				case VALUE_STRING:
				case VALUE_NUMBER_INT:
				case VALUE_NUMBER_FLOAT:
				case VALUE_FALSE:
				case VALUE_TRUE:
					String key = (currentPath + fieldName).substring(1, currentPath.length() + fieldName.length());
					values.get(name).put(key, parser.getText());
				default:
					// do nothing
				}
			}
		}
		return new QuestPackage(packName, values);
	}

	public void printMainYAML(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		// save NPCs
		if (!npcBindings.isEmpty()) {
			ObjectNode npcs = mapper.createObjectNode();
			for (NpcBinding binding : npcBindings) {
				npcs.put(binding.getId().get(), binding.getConversation().get().getId().get());
			}
			root.set("npcs", npcs);
		}
		// save global variables
		if (!variables.isEmpty()) {
			ObjectNode variables = mapper.createObjectNode();
			for (GlobalVariable var : this.variables) {
				variables.put(var.getId().get(), var.getInstruction().get());
			}
			root.set("variables", variables);
		}
		// save static events
		if (!staticEvents.isEmpty()) {
			ObjectNode staticEvents = mapper.createObjectNode();
			for (StaticEvent event : this.staticEvents) {
				staticEvents.put(event.getId().get(), event.getEvent().get().getId().get());
			}
			root.set("static", staticEvents);
		}
		// save global locations
		if (!locations.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (GlobalLocation loc : locations) {
				builder.append(loc.toString() + ",");
			}
			root.put("global_locations", builder.toString().substring(0, builder.length() - 1));
		}
		// save quest cancelers
		if (!cancelers.isEmpty()) {
			ObjectNode cancelers = mapper.createObjectNode();
			for (QuestCanceler canceler : this.cancelers) {
				ObjectNode cancelerNode = mapper.createObjectNode();
				addTranslatedNode(mapper, cancelerNode, "name", canceler.getName());
				if (!canceler.getEvents().isEmpty()) {
					StringBuilder events = new StringBuilder();
					for (IdWrapper<Event> event : canceler.getEvents()) {
						events.append(event.toString() + ',');
					}
					cancelerNode.put("events", events.toString().substring(0, events.length() - 1));
				}
				if (!canceler.getConditions().isEmpty()) {
					StringBuilder conditions = new StringBuilder();
					for (ConditionWrapper condition : canceler.getConditions()) {
						conditions.append(condition.toString() + ',');
					}
					cancelerNode.put("conditions", conditions.toString().substring(0, conditions.length() - 1));
				}
				if (!canceler.getObjectives().isEmpty()) {
					StringBuilder objectives = new StringBuilder();
					for (IdWrapper<Objective> objective : canceler.getObjectives()) {
						objectives.append(objective.toString() + ',');
					}
					cancelerNode.put("objectives", objectives.toString().substring(0, objectives.length() - 1));
				}
				if (!canceler.getTags().isEmpty()) {
					StringBuilder tags = new StringBuilder();
					for (IdWrapper<Tag> tag : canceler.getTags()) {
						tags.append(tag.toString() + ',');
					}
					cancelerNode.put("tags", tags.toString().substring(0, tags.length() - 1));
				}
				if (!canceler.getPoints().isEmpty()) {
					StringBuilder points = new StringBuilder();
					for (IdWrapper<PointCategory> point : canceler.getPoints()) {
						points.append(point.toString() + ',');
					}
					cancelerNode.put("points", points.toString().substring(0, points.length() - 1));
				}
				if (!canceler.getJournal().isEmpty()) {
					StringBuilder journals = new StringBuilder();
					for (IdWrapper<JournalEntry> journal : canceler.getJournal()) {
						journals.append(journal.toString() + ',');
					}
					cancelerNode.put("journal", journals.toString().substring(0, journals.length() - 1));
				}
				if (canceler.getLocation() != null) {
					cancelerNode.put("loc", canceler.getLocation());
				}
				cancelers.set(canceler.getId().get(), cancelerNode);
			}
			root.set("cancel", cancelers);
		}
		// save main page
		if (!mainPage.isEmpty()) {
			ObjectNode lines = mapper.createObjectNode();
			for (MainPageLine line : mainPage) {
				ObjectNode node = mapper.createObjectNode();
				addTranslatedNode(mapper, node, "text", line.getText());
				node.put("priority", line.getPriority().get());
				StringBuilder conditions = new StringBuilder();
				for (ConditionWrapper condition : line.getConditions()) {
					conditions.append(condition.toString() + ',');
				}
				node.put("conditions", conditions.substring(0, conditions.length() - 1));
				lines.set(line.getId().get(), node);
			}
			root.set("journal_main_page", lines);
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printEventsYaml(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		for (Event event : events) {
			root.put(event.getId().get(), event.getInstruction().get());
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printConditionsYaml(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		for (Condition condition : conditions) {
			root.put(condition.getId().get(), condition.getInstruction().get());
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printObjectivesYaml(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		for (Objective objective : objectives) {
			root.put(objective.getId().get(), objective.getInstruction().get());
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printItemsYaml(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		for (Item item : items) {
			root.put(item.getId().get(), item.getInstruction().get());
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printJournalYaml(OutputStream out) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		for (JournalEntry entry : journal) {
			addTranslatedNode(mapper, root, entry.getId().get(), entry.getText());
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	public void printConversationYaml(OutputStream out, Conversation conv) throws IOException {
		YAMLFactory yf = new YAMLFactory();
		YAMLMapper mapper = new YAMLMapper();
		ObjectNode root = mapper.createObjectNode();
		addTranslatedNode(mapper, root, "quester", conv.getNPC());
		root.put("stop", String.valueOf(conv.getStop().get()));
		StringBuilder first = new StringBuilder();
		for (IdWrapper<NpcOption> option : conv.getStartingOptions()) {
			first.append(option.toString() + ',');
		}
		root.put("first", first.substring(0, first.length() - 1));
		if (!conv.getFinalEvents().isEmpty()) {
			StringBuilder finalEvents = new StringBuilder();
			for (IdWrapper<Event> event : conv.getFinalEvents()) {
				finalEvents.append(event.toString() + ',');
			}
			root.put("final", finalEvents.substring(0, finalEvents.length() - 1));
		}
		if (!conv.getNpcOptions().isEmpty()) {
			ObjectNode npcOptions = mapper.createObjectNode();
			for (NpcOption option : conv.getNpcOptions()) {
				ObjectNode npcOption = mapper.createObjectNode();
				addTranslatedNode(mapper, npcOption, "text", option.getText());
				if (!option.getEvents().isEmpty()) {
					StringBuilder events = new StringBuilder();
					for (IdWrapper<Event> event : option.getEvents()) {
						events.append(event.toString() + ',');
					}
					npcOption.put("events", events.substring(0, events.length() - 1));
				}
				if (!option.getConditions().isEmpty()) {
					StringBuilder conditions = new StringBuilder();
					for (IdWrapper<Condition> condition : option.getConditions()) {
						conditions.append(condition.toString() + ',');
					}
					npcOption.put("conditions", conditions.substring(0, conditions.length() - 1));
				}
				if (!option.getPointers().isEmpty()) {
					StringBuilder pointers = new StringBuilder();
					for (IdWrapper<ConversationOption> pointer : option.getPointers()) {
						pointers.append(pointer.toString() + ',');
					}
					npcOption.put("pointers", pointers.substring(0, pointers.length() - 1));
				}
				npcOptions.set(option.getId().get(), npcOption);
			}
			root.set("NPC_options", npcOptions);
		}
		if (!conv.getPlayerOptions().isEmpty()) {
			ObjectNode playerOptions = mapper.createObjectNode();
			for (PlayerOption option : conv.getPlayerOptions()) {
				ObjectNode playerOption = mapper.createObjectNode();
				addTranslatedNode(mapper, playerOption, "text", option.getText());
				if (!option.getEvents().isEmpty()) {
					StringBuilder events = new StringBuilder();
					for (IdWrapper<Event> event : option.getEvents()) {
						events.append(event.toString() + ',');
					}
					playerOption.put("events", events.substring(0, events.length() - 1));
				}
				if (!option.getConditions().isEmpty()) {
					StringBuilder conditions = new StringBuilder();
					for (IdWrapper<Condition> condition : option.getConditions()) {
						conditions.append(condition.toString() + ',');
					}
					playerOption.put("conditions", conditions.substring(0, conditions.length() - 1));
				}
				if (!option.getPointers().isEmpty()) {
					StringBuilder pointers = new StringBuilder();
					for (IdWrapper<ConversationOption> pointer : option.getPointers()) {
						pointers.append(pointer.toString() + ',');
					}
					playerOption.put("pointers", pointers.substring(0, pointers.length() - 1));
				}
				playerOptions.set(option.getId().get(), playerOption);
			}
			root.set("player_options", playerOptions);
		}
		yf.createGenerator(out).setCodec(mapper).writeObject(root);
	}

	private void addTranslatedNode(YAMLMapper mapper, ObjectNode root, String name, TranslatableText text) {
		if (text.getDef() != null) {
			root.put(name, text.getDef().get());
		} else {
			ObjectNode node = mapper.createObjectNode();
			for (String lang : text.getLanguages()) {
				if (lang == null) { // TODO find out why there's a null language
					continue;
				}
				node.put(lang, text.get(lang).get());
			}
			root.set(name, node);
		}
	}

	/**
	 * Saves the package to a .zip file.
	 * 
	 * @param zipFile
	 */
	public void saveToZip(File zip) {
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
			String prefix = packName.get() + File.separator;
			// save main.yml file
			ZipEntry main = new ZipEntry(prefix + "main.yml");
			out.putNextEntry(main);
			printMainYAML(out);
			out.closeEntry();
			// save conversation files
			for (Conversation conv : conversations) {
				ZipEntry conversation = new ZipEntry(
						prefix + "conversations" + File.separator + conv.getId().get() + ".yml");
				out.putNextEntry(conversation);
				printConversationYaml(out, conv);
				out.closeEntry();
			}
			// save events.yml file
			ZipEntry events = new ZipEntry(prefix + "events.yml");
			out.putNextEntry(events);
			printEventsYaml(out);
			out.closeEntry();
			// save conditions.yml file
			ZipEntry conditions = new ZipEntry(prefix + "conditions.yml");
			out.putNextEntry(conditions);
			printConditionsYaml(out);
			out.closeEntry();
			// save objectives.yml file
			ZipEntry objectives = new ZipEntry(prefix + "objectives.yml");
			out.putNextEntry(objectives);
			printObjectivesYaml(out);
			out.closeEntry();
			// save items.yml file
			ZipEntry items = new ZipEntry(prefix + "items.yml");
			out.putNextEntry(items);
			printItemsYaml(out);
			out.closeEntry();
			// save journal.yml file
			ZipEntry journal = new ZipEntry(prefix + "journal.yml");
			out.putNextEntry(journal);
			printJournalYaml(out);
			out.closeEntry();
			// done
			out.close();
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}

}
