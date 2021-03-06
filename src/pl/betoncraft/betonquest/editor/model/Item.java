/**
 * 
 */
package pl.betoncraft.betonquest.editor.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import pl.betoncraft.betonquest.editor.controller.InstructionEditController;
import pl.betoncraft.betonquest.editor.data.ID;
import pl.betoncraft.betonquest.editor.data.Instruction;
import pl.betoncraft.betonquest.editor.data.SimpleID;

/**
 * Represents an item.
 *
 * @author Jakub Sapalski
 */
public class Item extends SimpleID implements Instruction {

	private StringProperty instruction = new SimpleStringProperty();
	
	public Item(QuestPackage pack, String id) {
		this.pack = ID.parsePackage(pack, id);
		this.id = new SimpleStringProperty(ID.parseId(id));
	}
	
	public Item(QuestPackage pack, String id, String instruction) {
		this(pack, id);
		this.instruction.set(instruction);
	}
	
	@Override
	public boolean edit() {
		return InstructionEditController.display(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ObservableList<Item> getList() {
		return pack.getItems();
	}

	@Override
	public StringProperty getInstruction() {
		return instruction;
	}

}
