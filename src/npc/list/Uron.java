package npc.list;
import npc.Npc;
import player.Player;
import shop.ShopService;

public class Uron extends Npc {

    public Uron(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public boolean canSeeNpc(Player pl) {
        return pl.playerTask.taskMain.id >= 9;
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canSeeNpc(pl)) {
            super.openBaseMenu(pl);
            ShopService.gI().opendShop(pl, "URON", false);
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canSeeNpc(player)) {
            // future actions if needed
        }
    }
}
