package ru.alexanderdv.modcraft;

import java.util.ArrayList;
import java.util.Scanner;

import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.configs.SConfig;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;

public class Commands {

	public static interface ToServerSender { void sendToServer(String executor, String cmd); }

	ToServerSender toServerSender;
	WorldEdit worldEdit;
	UserController player;
	Thread systemConsoleScannerThread;
	Scanner systemConsoleScanner;
	SConfig permissions, commands;
	Time time;

	public Commands(ToServerSender toServerSender, UserController player, WorldEdit worldEdit, Time time) {
		this.toServerSender = toServerSender;
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
	}

	public String command(String executor, String line) {
		if (line.equals(""))
			return line;
		if (executor.equals(player.getName()))
			toServerSender.sendToServer(executor, line);
		String result = perform(executor, line);
		if (!result.equals(""))
			Msgs.last.debug(result);
		return result;
	}

	protected boolean hasPermission(String executor, String permission) {
		if (("," + permissions.get("default") + ",").contains("," + permission + ","))
			return true;
		return ("," + permissions.get(executor) + ",").contains("," + permission + ",");
	}

	ArrayList<String> lastCommands = new ArrayList<>();

	protected String perform(String executor, String line) { return perform(executor, line, 0); }

	VectorD pos, pos2;

	protected String perform(String executor, String line, int recursion) {
		String[] args = line.split(" ");
		String cmd = args[0].replace("/", "");
		if (args.length == 1 && args[0].replaceAll("[0-9.,+-]+", "").equals(""))
			return perform(executor, MathUtils.loopGet(lastCommands, MathUtils.parseI(args[0])));
		if (recursion < 5)
			for (String macros : commands.keySet())
				if (("," + macros.toLowerCase() + ",").contains("," + args[0].toLowerCase() + ",")) {
					String result = "";
					for (String bind : commands.get(macros).split(","))
						result += perform(executor, bind + line.substring(args[0].length()), bind.toLowerCase().startsWith(args[0].toLowerCase()) ? 5 : recursion + 1) + ";";
					return result;
				}
		if (!hasPermission(executor, cmd))
			return "Not enough permissions of '" + executor + "' for command '" + line + "'!";
		lastCommands.add(line);
		if (pos == null)
			pos = new VectorD(player.position.size());
		if (pos2 == null)
			pos2 = new VectorD(player.position.size());
		if (args.length > 2)
			for (int i = 0; i < player.position.size(); i++)
				pos.coords[i] = args.length < 1 + i + 1 ? player.position.coords[i] : MathUtils.parseD(args[1 + i]) + (args[1 + i].startsWith("~") ? player.position.coords[i] : 0);
		if (cmd.equalsIgnoreCase("pos1")) {
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
		return "Command '" + line + "' of '" + executor + "' performed";
	}

	public void close() { systemConsoleScannerThread.interrupt(); }
}
