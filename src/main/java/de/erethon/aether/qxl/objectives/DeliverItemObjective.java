package de.erethon.aether.qxl.objectives;

import de.erethon.aether.events.MobDeliverItemEvent;
import de.erethon.hephaestus.items.HItem;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QBaseObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "deliver_item",
        description = "Deliver a specific item to a mob to complete this objective.",
        shortExample = "deliver_item: item=minecraft:diamond; amount=5",
        longExample = {
                "deliver_item:",
                "  item: minecraft:diamond # The item that needs to be delivered.",
                "  amount: 5               # The amount of the item that needs to be delivered."
        }
)
public class DeliverItemObjective extends QBaseObjective<MobDeliverItemEvent> {

    @QParamDoc(name = "mob", description = "The ID of the mob to which the item must be delivered.", required = true)
    private String mobID;
    @QParamDoc(name = "item", description = "The Hephaestus ID of the item that needs to be delivered.", required = true)
    private String itemID;
    @QParamDoc(name = "amount", description = "The amount of the item that needs to be delivered.", def = "1")
    private int amount;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return null;
    }

    @Override
    public Class<MobDeliverItemEvent> getEventType() {
        return MobDeliverItemEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, MobDeliverItemEvent event) {
        if (!conditions(event.getPlayer())) {
            return;
        }
        if (!event.getMobID().equalsIgnoreCase(mobID)) {
            return;
        }
        HItem deliveredItem = event.getItem();
        if (deliveredItem.getKey().toString().equalsIgnoreCase(itemID) && event.getAmount() >= amount) {
            event.setAmount(event.getAmount() - amount);
            checkCompletion(activeObjective, this, getPlayerHolder(event.getPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        itemID = cfg.getString("item");
        mobID = cfg.getString("mob");
        amount = cfg.getInt("amount", 1);
    }
}
