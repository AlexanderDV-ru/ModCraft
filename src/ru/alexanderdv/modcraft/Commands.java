package ru.alexanderdv.modcraft;

import java.util.ArrayList;
import java.util.Scanner;

import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.Controller.UserController;
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

	public Commands(ToServerSender toServerSender, UserController player, WorldEdit worldEdit) {
		this.toServerSender = toServerSender;
		this.player = player;
		this.worldEdit = worldEdit;
		permissions = new SConfig("configs/permissions.cfg");
		systemConsoleScannerThread = new Thread(() -> {
			systemConsoleScanner = new Scanner(System.in);
			while (systemConsoleScanner.hasNextLine())
				command("console", systemConsoleScanner.nextLine());
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

	SConfig permissions;

	protected boolean hasPermission(String executor, String permission) {
		if (("," + permissions.get("default") + ",").contains("," + permission + ","))
			return true;
		return ("," + permissions.get(executor) + ",").contains("," + permission + ",");
	}

	ArrayList<String> lastCommands = new ArrayList<>();

	protected String perform(String executor, String line) {
		String[] args = line.split(" ");
		String cmd = args[0];
		if (args.length == 1 && args[0].replaceAll("[0-9.,+-]+", "").equals(""))
			return perform(executor, MathUtils.loopGet(lastCommands, MathUtils.parseI(args[0])));
		if (!hasPermission(executor, cmd))
			return "Executor '" + executor + "' don't have enough permissions to perform command '" + line + "'!";
		lastCommands.add(line);
		VectorD pos = new VectorD(MathUtils.parseD(args[1]), MathUtils.parseD(args[2]), MathUtils.parseD(args[3]));
		for (int i = 0; i < 3; i++)
			if (args[1 + i].startsWith("~"))
				pos.coords[i] += player.position.coords[i];
		Msgs.last.debug(pos.coords);
		if (cmd.equalsIgnoreCase("setblock"))
			worldEdit.world.setBlock((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), args.length < 5 ? player.idInHand : MathUtils.parseI(args[4]));
		else if (cmd.equalsIgnoreCase("teleport") || cmd.equalsIgnoreCase("tp"))
			player.position.coords = new double[] { (int) pos.getX(), (int) pos.getY(), (int) pos.getZ() };
		else if (cmd.equalsIgnoreCase("explosion"))
			worldEdit.createExplosion((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), args.length < 5 ? 5 : MathUtils.parseI(args[4]));
		else if (cmd.equalsIgnoreCase("sphere"))
			worldEdit.sphere((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), MathUtils.parseD(args[4]), args.length < 6 ? player.idInHand : MathUtils.parseI(args[5]), args.length < 7 ? false : args[6].equals("true"));
		else return "Unknown command '" + line + "'";
		return "Command '" + line + "' performed";
	}

	public void close() { systemConsoleScannerThread.interrupt(); }
}
