package us.corenetwork.challenges.usercommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.corenetwork.challenges.PlayerPoints;
import us.corenetwork.challenges.PlayerRank;
import us.corenetwork.challenges.Setting;
import us.corenetwork.challenges.Settings;
import us.corenetwork.challenges.Util;


public class PointsCommand extends BaseUserCommand {
	
	public PointsCommand()
	{
		desc = "See your points";
		needPlayer = true;
		permission = "points";
	}


	public Boolean run(CommandSender sender, String[] args) {
		int points = PlayerPoints.getPoints(((Player) sender).getName());
		PlayerRank curRank = PlayerPoints.getRank(points);
		PlayerRank nextRank = PlayerPoints.getNextRank(curRank);
		String message;
		if (nextRank == null)
			message = Settings.getString(Setting.MESSAGE_FLATPOINTS);
		else
		{
			message = Settings.getString(Setting.MESSAGE_FLATPOINTS_NEXT_RANK);
			message = message.replace("<NewRank>", nextRank.rank);
			message = message.replace("<PointsLeft>", Integer.toString(nextRank.neededPoints - points));
		}
		message = message.replace("<Points>", Integer.toString(points));
		message = message.replace("<Rank>", curRank.rank);

		Util.Message(message, sender);
		
		return true;
	}
}
