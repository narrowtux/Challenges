package us.corenetwork.challenges;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerPoints {
	public static int getPoints(String name)
	{
		int points = 0;
		try {
			PreparedStatement statement = IO.getConnection().prepareStatement("SELECT Points FROM player_points WHERE Player = ? LIMIT 1");
			statement.setString(1, name);
			ResultSet set = statement.executeQuery();
			if (set.next())
			{
				points = set.getInt("Points");
			}
			
			set.close();
			statement.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return points;
	}
	
	public static void setPoints(String name, Integer points)
	{
		try {
			PreparedStatement statement = IO.getConnection().prepareStatement("DELETE FROM player_points WHERE Player = ?");
			statement.setString(1, name);
			statement.executeUpdate();
			statement.close();
			
			statement = IO.getConnection().prepareStatement("INSERT INTO player_points (Player, Points) VALUES (?,?)");
			statement.setString(1, name);
			statement.setInt(2, points);
			statement.executeUpdate();
			statement.close();
			
			IO.getConnection().commit();

		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	
	public static void addPoints(String name, Integer amount, String reason)
	{
		Player player = Bukkit.getServer().getPlayerExact(name);
		if (player != null)
		{
			addPoints(player, amount, reason, null);
			return;
		}
		
		PreparedStatement statement;
		try {
			statement = IO.getConnection().prepareStatement("INSERT INTO point_changes (Player, Amount, Reason) VALUES (?,?,?)");
			statement.setString(1, name);
			statement.setInt(2, amount);
			statement.setString(3, reason);

			statement.executeUpdate();
			statement.close();
					
			IO.getConnection().commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public static PlayerRank getRank(Integer points)
	{
		if (points < 0)
			points = 0;
		
		PlayerRank curRank = null;
		
		for (PlayerRank rank : IO.ranks)
		{
			if (rank.neededPoints <= points)
			{
				curRank = rank;
			}
		}
		
		return curRank;
	}
	
	public static PlayerRank getNextRank(PlayerRank currentRank)
	{
		PlayerRank winner = null;
		
		for (PlayerRank rank : IO.ranks)
		{
			if (rank.neededPoints > currentRank.neededPoints && (winner == null || rank.neededPoints < winner.neededPoints))
			{
				winner = rank;
			}
		}
		
		return winner;
	}
	
	public static void addPoints(Player player, Integer amount, String reason, Integer id)
	{
		String name = player.getName();
		
		int curPoints = getPoints(player.getName());
		PlayerRank oldRank = getRank(curPoints);
		PlayerRank newRank = getRank(curPoints + amount);
				
		if (oldRank != newRank)
		{			
			World firstWorld = Bukkit.getServer().getWorlds().get(0);

			Permission permission = Challenges.permission;
			Chat chat = Challenges.chat;
			
			boolean dontChange = false;

			// check if the player is in a protected group
			for (String group : (List<String>) Settings.getList(Setting.PROTECTED_GROUPS))
			{
					if (permission.playerInGroup(player, group))
					{
						dontChange = true;
						break;

					}
			}

			// TODO do something with guardians

			permission.playerRemoveGroup(firstWorld, player.getName(), oldRank.group);
			permission.playerAddGroup(firstWorld, player.getName(), newRank.group);

			chat.setPlayerSuffix(player, dontChange ? "" : newRank.suffix);
									
			String message;
			String jubilation = Util.getRandomItem((ArrayList<String>) Settings.getProperty(Setting.JUBILATIONS));
			if (amount > 0)
			{
				if (reason == null)
				{
					message = Settings.getString(Setting.MESSAGE_PROMOTED);
				}
				else
				{
					message = Settings.getString(Setting.MESSAGE_PROMOTED_REASON);
					message = message.replace("<Reason>", reason);
				}
				message = message.replace("<Jubilation>", jubilation);
				
				String globalMessage = Settings.getString(Setting.MESSAGE_GLOBAL_PROMOTED);
				globalMessage = globalMessage.replace("<Player>", name);
				globalMessage = globalMessage.replace("<Class>", newRank.rank);
				globalMessage = globalMessage.replace("<Jubilation>", jubilation);
				
				Util.Broadcast(globalMessage, name);
			}
			else
			{
				if (reason == null)
				{
					message = Settings.getString(Setting.MESSAGE_DEMOTED);
				}
				else
				{
					message = Settings.getString(Setting.MESSAGE_DEMOTED_REASON);
					message = message.replace("<Reason>", reason);
					
					String globalMessage = Settings.getString(Setting.MESSAGE_GLOBAL_DEMOTED);
					globalMessage = globalMessage.replace("<Player>", name);
					globalMessage = globalMessage.replace("<Reason>", reason);
					globalMessage = globalMessage.replace("<Class>", newRank.rank);
					globalMessage = globalMessage.replace("<Points>", Integer.toString(-amount));
					Util.Broadcast(globalMessage, name);

				}
			}
			
			
			message = message.replace("<Class>", newRank.rank);
			Util.Message(message, player);
		}
		else if (amount > 0)
		{
			String message;
			if (reason == null)
			{
				message = Settings.getString(Setting.MESSAGE_EARNED_POINTS);
			}
			else
			{
				message = Settings.getString(Setting.MESSAGE_EARNED_POINTS_REASON);
				message = message.replace("<Reason>", reason);
			}
			
			message = message.replace("<Amount>", Integer.toString(amount));
			Util.Message(message, player);

		}
		else
		{
			String message;
			if (reason == null)
			{
				message = Settings.getString(Setting.MESSAGE_PUNISHED);
				
			}
			else
			{
				message = Settings.getString(Setting.MESSAGE_PUNISHED_REASON);
				message = message.replace("<Reason>", reason);
				
				String globalMessage = Settings.getString(Setting.MESSAGE_GLOBAL_PUNISHED);
				globalMessage = globalMessage.replace("<Player>", name);
				globalMessage = globalMessage.replace("<Points>", Integer.toString(-amount));
				globalMessage = globalMessage.replace("<Reason>", reason);
				Util.Broadcast(globalMessage, name);
			}
			
			message = message.replace("<Amount>", Integer.toString(-amount));
			Util.Message(message, player);
		}
		
		setPoints(player.getName(), getPoints(player.getName()) + amount);
		
		if (id != null)
		{
			try
			{
				PreparedStatement statement = IO.getConnection().prepareStatement("DELETE FROM point_changes WHERE ID = ?");
				statement.setInt(1, id);
				statement.executeUpdate();
				statement.close();
				
				IO.getConnection().commit();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static PlayerRank getNewRank(String player, Integer change)
	{
		int curPoints = getPoints(player);
		PlayerRank currentRank = getRank(curPoints);
		PlayerRank newRank = getRank(curPoints + change);
				
		return currentRank != newRank ? newRank : null;
	}
	
	
}
