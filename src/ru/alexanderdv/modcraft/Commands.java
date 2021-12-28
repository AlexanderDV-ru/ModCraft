package ru.alexanderdv.modcraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.Timer;

import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.Input.DisplayInput;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.modcraft.configs.SConfig;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;

public class Commands {

	String args;
	UserController player;
	WorldEdit worldEdit;
	Time time;

	Thread systemConsoleScannerThread;
	Scanner systemConsoleScanner;
	SConfig permissions, commands;

	public Commands(String args, UserController player, WorldEdit worldEdit, Time time) {
		this.args = args;
		this.player = player;
		this.worldEdit = worldEdit;
		this.time = time;

		permissions = new SConfig("configs/permissions.cfg");
		commands = new SConfig("configs/commands.cfg");
		systemConsoleScannerThread = new Thread(() -> {
			systemConsoleScanner = new Scanner(System.in);
			while (systemConsoleScanner.hasNextLine())
				try {
					command("console", systemConsoleScanner.nextLine());
				} catch (Exception e) {
					Msgs.last.debug(e);
				}
			systemConsoleScanner.close();
		});
		systemConsoleScannerThread.start();

		new Timer(1000, (a) -> {
//			try {
//				client.closeAll();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			client.startInExceptionThread(() -> client.startClient(client.name, args.split("-serverip:")[1].split(" ")[0], MathUtils.parseI(args.split("-serverport:")[1].split(" ")[0])));
		}).start();
	}

	public String command(String executor, String line) {
		if (line.equals(""))
			return line;
		if (executor.equals(player.getName()))
			sendToServer(executor, line);
		String result = perform(executor, line);
		if (!result.equals("") && !result.contains("update") && (dontDebugPlayerBlockBreakAndPlace ? executor.equalsIgnoreCase(player.getName()) : true))
			Msgs.last.debug(result);
		return result;
	}

	protected boolean hasPermission(String executor, String permission) {
		if (("," + permissions.get("default") + ",").contains("," + permission + ","))
			return true;
		if (executor.equalsIgnoreCase("console"))
			if (!("," + permissions.get("console") + ",").contains(",!" + permission + ","))
				return true;
		return ("," + permissions.get(executor) + ",").contains("," + permission + ",");
	}

	ArrayList<String> lastCommands = new ArrayList<>();

	protected String perform(String executor, String line) { return perform(executor, line, 0); }

	HashMap<String, VectorD> firstPositions = new HashMap<>(), secondPositions = new HashMap<>();
	HashMap<String, PhysicalPOV> players = new HashMap<>();
	Networking client, server;
	public boolean smoothMotion = true;
	public boolean dontDebugPlayerBlockBreakAndPlace;

	protected String perform(String executor, String line, int recursion) {
		String result = "";
		String[] args = line.split(" ");
		String cmd = args[0].replace("/", "").replace("-", "").replace(".", "").replace("_", "");
		if (args.length == 1 && args[0].replaceAll("[0-9.,+-]+", "").equals(""))
			return perform(executor, MathUtils.loopGet(lastCommands, MathUtils.parseI(args[0])));
		if (recursion < 5)
			for (String macros : commands.keySet())
				if (("," + macros.toLowerCase() + ",").contains("," + args[0].toLowerCase() + ",")) {
					for (String bind : commands.get(macros).split(","))
						result += perform(executor, bind + line.substring(args[0].length()), bind.toLowerCase().startsWith(args[0].toLowerCase()) ? 5 : recursion + 1) + ";";
					return result;
				}
		if (!hasPermission(executor, cmd))
			return "Not enough permissions of '" + executor + "' for command '" + line + "'!";
		lastCommands.add(line);
		if (firstPositions.get(executor) == null)
			firstPositions.put(executor, new VectorD(player.position.size()));
		if (secondPositions.get(executor) == null)
			secondPositions.put(executor, new VectorD(player.position.size()));
		VectorD pos = firstPositions.get(executor), pos2 = secondPositions.get(executor);
		if (args.length > 2)
			for (int i = 0; i < player.position.size(); i++)
				pos.coords[i] = args.length < 1 + i + 1 ? player.position.coords[i] : MathUtils.parseD(args[1 + i]) + (args[1 + i].startsWith("~") ? player.position.coords[i] : 0);
		if (cmd.equalsIgnoreCase("startserver")) {
			try {
				server = new Networking();
				server.startInExceptionThread(() -> server.startServer(player.getName(), MathUtils.parseI(args.length == 1 ? this.args.split("-port:")[1].split(" ")[0] : args[1])), (e) -> {

					Msgs.last.debug(e);
				});
				// result += server.server.getInetAddress() + ":" + server.server.getLocalPort()
				// + ";";
				if (server.socket != null)
					result += server.socket.getInetAddress() + ":" + server.socket.getPort() + " (" + server.socket.getLocalAddress() + ":" + server.socket.getLocalPort() + ")" + ";";
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
		} else if (cmd.equalsIgnoreCase("startclient")) {
			try {
				client = new Networking();
				// result += client.socket.getInetAddress() + ":" + client.socket.getPort() + "
				// (" + client.socket.getLocalAddress() + ":" + client.socket.getLocalPort() +
				// ")" + ";";
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
		} else if (cmd.equalsIgnoreCase("sendworldtoserver")) {
			player.doForEachSeenBlock((x, y, z, w) -> { client.writeRequests.put("setblock " + x + " " + y + " " + z + " " + w + " " + worldEdit.world.getBlock(x, y, z, w).getId(), ""); });
			sendToServer(executor, "sendingworldblocks");
		} else if (cmd.equalsIgnoreCase("updateplayerpos")) {
			if (players.get(executor) == null)
				players.put(executor, new PhysicalPOV());
			PhysicalPOV player = players.get(executor);
			for (int i = 0; i < player.position.size(); i++)
				if (Double.parseDouble(args[1 + i]) - player.position.coords[i] > 10 || !smoothMotion)
					player.position.coords[i] = Double.parseDouble(args[1 + i]);
			for (int i = 0; i < player.velocity.size(); i++)
				player.velocity.coords[i] = ((+(Double.parseDouble(args[1 + 4 + i]) + (Double.parseDouble(args[1 + i]) - player.position.coords[i]))));
			player.gravityMass = Double.parseDouble(args[1 + 4 + 4]);
			if (smoothMotion)
				player.inertia = 30;
		} else if (cmd.equalsIgnoreCase("pos1")) {
			for (int i = 0; i < player.position.size(); i++)
				pos.coords[i] = args.length < 1 + i + 1 ? player.position.coords[i] : MathUtils.parseD(args[1 + i]) + (args[1 + i].startsWith("~") ? player.position.coords[i] : 0);
		} else if (cmd.equalsIgnoreCase("pos2")) {
			for (int i = 0; i < player.position.size(); i++)
				pos2.coords[i] = args.length < 1 + i + 1 ? player.position.coords[i] : MathUtils.parseD(args[1 + i]) + (args[1 + i].startsWith("~") ? player.position.coords[i] : 0);
		} else if (cmd.equalsIgnoreCase("setblock")) {
			if (args.length < 3)
				worldEdit.world.setBlocks((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), (int) pos.getW(),

						(int) pos2.getX(), (int) pos2.getY(), (int) pos2.getZ(), (int) pos2.getW(),

						args.length < 6 - 4 ? player.inventory[player.selectedSlot] : MathUtils.parseI(args[5 - 4]));
			else if (args.length < 8)
				worldEdit.world.setBlock((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), (int) pos.getW(), args.length < 6 ? player.inventory[player.selectedSlot] : MathUtils.parseI(args[5]));
			else {
				for (int i = 0; i < player.position.size(); i++)
					pos2.coords[i] = args.length < 4 + 1 + i + 1 ? player.position.coords[i] : MathUtils.parseD(args[4 + 1 + i]) + (args[4 + 1 + i].startsWith("~") ? player.position.coords[i] : 0);
				worldEdit.world.setBlocks((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), (int) pos.getW(),

						(int) pos2.getX(), (int) pos2.getY(), (int) pos2.getZ(), (int) pos2.getW(),

						args.length < 6 ? player.inventory[player.selectedSlot] : MathUtils.parseI(args[5 + 4]));
			}
		} else if (cmd.equalsIgnoreCase("setposition"))
			for (int i = 0; i < player.position.size(); i++)
				player.position.coords[i] = pos.coords[i];
		else if (cmd.equalsIgnoreCase("setvelocity"))
			for (int i = 0; i < player.velocity.size(); i++)
				player.velocity.coords[i] = pos.coords[i];
		else if (cmd.equalsIgnoreCase("setvision"))
			for (int i = 0; i < player.vision.size(); i++)
				player.vision.coords[i] = pos.coords[i];
		else if (cmd.equalsIgnoreCase("setsize"))
			for (int i = 0; i < player.size.size(); i++)
				player.size.coords[i] = pos.coords[i];
		else if (cmd.equalsIgnoreCase("explosion"))
			worldEdit.createExplosion((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), (int) pos.getW(), args.length < 6 ? player.tntExplosionRadius : MathUtils.parseI(args[5]));
		else if (cmd.equalsIgnoreCase("sphere"))
			worldEdit.sphere((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), (int) pos.getW(), MathUtils.parseD(args[5]), args.length < 7 ? player.inventory[player.selectedSlot] :

					MathUtils.parseI(args[6]), args.length < 8 ? false : args[7].equals("true"));
		else if (cmd.equalsIgnoreCase("settimescale"))
			time.changePhysicalScaleTo(MathUtils.parseD(args[1]));
		else return "Unknown command '" + line + "' of '" + executor + "'";
		return "Command '" + line + "' of '" + executor + "' performed:;" + result;
	}

	public boolean isServer() { return !(args.split("-port:").length < 2 || args.split("-port:")[1].split(" ")[0].length() < 1); }

	public boolean isClient() { return !(args.split("-serverip:").length < 2 || args.split("-serverip:")[1].split(" ")[0].length() < 1); }

	public void sendToServer(String executor, String cmd) {
		if (!isClient())
			return;
		client.writeRequests.put(cmd, "");
		client.name = executor;
		client.startInExceptionThread(() -> client.startClient(client.name, args.split("-serverip:")[1].split(" ")[0], MathUtils.parseI(args.split("-serverport:")[1].split(" ")[0])));
	}

	public void performWaitingCommands(DisplayInput mainInput) {
		try {
			for (String cmd : server.readenRequests.keySet())
				command(server.readenRequests.get(cmd), cmd);
			server.readenRequests.clear();
		} catch (Exception e) {}
		try {
			for (String cmd : client.readenRequests.keySet())
				command(client.readenRequests.get(cmd), cmd);
			client.readenRequests.clear();
		} catch (Exception e) {}
		for (String macros : commands.keySet())
			for (String macro : macros.split(","))
				if (macro.toLowerCase().startsWith("key_"))
					for (Key input : mainInput.nextKeys)
						if (input.isKeyDown(macro))
							command("keyboard", macro);
	}

	public void close() { systemConsoleScannerThread.interrupt(); }
}
