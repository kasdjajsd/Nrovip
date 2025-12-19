package server;

import consts.ConstNpc;
import managers.GiftCodeManager;
import item.Item;
import player.Pet;
import player.Player;
import network.SessionManager;
import services.ItemService;
import services.PetService;
import services.Service;
import services.func.Input;
import map.Service.ChangeMapService;
import map.Service.NpcService;
import player.Service.InventoryService;
import utils.SystemMetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import services.TaskService;

public class Command {

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }
    private void initAdminCommands() {
    adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
    adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
    adminCommands.put("hoiskill", player -> Service.gI().releaseCooldownSkill(player));
    adminCommands.put("boss", player -> {
    StringBuilder sb = new StringBuilder();
    sb.append("|1|Danh sách Boss hiện tại:\n");

    // Ví dụ danh sách boss, bạn có thể lấy từ danh sách thật trong hệ thống
    String[] bossNames = {
        "Tiểu đội trưởng", "Số 2", "Số 1", "Số 3", "Số 4",
        "Tiểu đội trưởng Namek", "Số 4 Namek", "Số 3 Namek", "Số 2 Namek"
    };

    for (int i = 0; i < bossNames.length; i++) {
        boolean isActive = (i % 3 == 0); // giả lập boss ACTIVE mỗi 3 dòng
        sb.append((i + 1))
          .append(". ")
          .append(bossNames[i])
          .append(" - ")
          .append(isActive ? "ACTIVE" : "AFK")
          .append("\n");
    }

    Service.gI().sendThongBaoOK(player, sb.toString());
});
adminCommands.put("baotri", player -> {
    int countdownSeconds = 15;

    // Bắt đầu luồng đếm ngược
    new Thread(() -> {
        try {
            for (int i = countdownSeconds; i >= 1; i--) {
                String message = i + " Giây nữa máy chủ sẽ bảo trì vui lòng thoát khỏi game để tránh mất đồ admin sẽ không giúp được gì khi bị mất đồ !";
                for (Player p : Client.gI().getPlayers()) {
                    Service.gI().sendThongBao(p, message);
                }
                Thread.sleep(1000); // đợi 1 giây
            }

            // Kick toàn bộ người chơi
            for (Player p : Client.gI().getPlayers()) {
                if (p.getSession() != null) {
                    p.getSession().disconnect();
                }
            }

            // Ghi log và tắt server
            System.out.println(">> Server shutting down by admin command.");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
});


    adminCommands.put("d", player -> Service.gI().setPos(player, player.location.x, player.location.y + 10));
    adminCommands.put("menu", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time start: " + ServerManager.timeStart 
                + "\nClients: " + Client.gI().getPlayers().size()
                + " người chơi\n Sessions: " + SessionManager.gI().getNumSession() 
                + "\nThreads: " + Thread.activeCount()
                + " luồng" + "\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Đóng"));
    }

private void initParameterizedCommands() {
    parameterizedCommands.put("m ", (player, text) -> {
        try {
            String data = text.replace("m ", "").trim();
            if (!data.isEmpty()) {
                int mapId = Integer.parseInt(data);
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            } else {
                Service.gI().sendThongBao(player, "Thiếu ID bản đồ!");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "ID bản đồ không hợp lệ!");
        }
    });

    parameterizedCommands.put("toado", (player, text) -> {
        Service.gI().sendThongBaoOK(player, "x: " + player.location.x + " - y: " + player.location.y);
    });

    parameterizedCommands.put("n", (player, text) -> {
        try {
            String data = text.replace("n", "").trim();
            if (!data.isEmpty()) {
                int idTask = Integer.parseInt(data);
                player.playerTask.taskMain.id = idTask - 1;
                player.playerTask.taskMain.index = 0;
                TaskService.gI().sendNextTaskMain(player);
            } else {
                Service.gI().sendThongBao(player, "Thiếu ID nhiệm vụ!");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "ID nhiệm vụ không hợp lệ!");
        }
    });

    parameterizedCommands.put("i", (player, text) -> {
        try {
            String raw = text.substring(1).trim(); // bỏ chữ "i"
            int itemId, quantity = 1;

            if (raw.contains("sl")) {
                String[] parts = raw.split("sl");
                itemId = Integer.parseInt(parts[0].trim());
                quantity = Integer.parseInt(parts[1].trim());
            } else {
                itemId = Integer.parseInt(raw);
            }

            Item item = ItemService.gI().createNewItem((short) itemId);
            List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
            if (!ops.isEmpty()) {
                item.itemOptions = ops;
            }
            item.quantity = quantity;

            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã nhận " + item.template.name + " [" + item.template.id + "] x" + quantity);
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Cú pháp sai! Ví dụ: i1sl999");
        }
    });
}

    public void chat(Player player, String text) {
        if (!check(player, text)) {
            Service.gI().chat(player, text);
        }
    }

    public boolean check(Player player, String text) {
        if (player.isAdmin()) {
            if (adminCommands.containsKey(text)) {
                adminCommands.get(text).accept(player);
                return true;
            }

            for (Map.Entry<String, BiConsumer<Player, String>> entry : parameterizedCommands.entrySet()) {
                if (text.startsWith(entry.getKey())) {
                    entry.getValue().accept(player, text);
                    return true;
                }
            }
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replaceAll("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
            }
        }
        return false;
    }
}
