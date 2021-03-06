package com.theminequest.MineQuest.Backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.theminequest.MineQuest.CompleteStatus;
import com.theminequest.MineQuest.MineQuest;
import com.theminequest.MineQuest.Backend.BackendFailedException.BackendReason;
import com.theminequest.MineQuest.BukkitEvents.QuestAvailableEvent;
import com.theminequest.MineQuest.Group.Team;
import com.theminequest.MineQuest.Quest.QuestDescription;
import com.theminequest.MineQuest.Quest.QuestManager;
import com.theminequest.MineQuest.Utils.ChatUtils;

public final class QuestBackend {

	/**
	 * Mark a quest as AVAILABLE for the player.
	 * @param p Player
	 * @param quest_name Quest Name
	 * @throws BackendFailedException If the backend failed to do its job.
	 */
	public static void giveQuestToPlayer(Player p, String quest_name)
			throws BackendFailedException {
		boolean repeatable = false;
		boolean regive = false;
		try {
			repeatable = isRepeatable(quest_name);
		} catch (IllegalArgumentException e){
			throw new BackendFailedException(e);
		}
		// check if the player already has this quest
		List<String> noncompletedquests;
		try {
			noncompletedquests = getQuests(QuestAvailability.ACCEPTED,p);
			noncompletedquests.addAll(getQuests(QuestAvailability.AVAILABLE,p));
		} catch (SQLException e) {
			MineQuest.log(Level.SEVERE, "[QuestBackend] Invoked giveQuestToPlayer by " +
					p.getName() + " on quest " + quest_name + " threw exception:");
			MineQuest.log(Level.SEVERE, e.toString());
			throw new BackendFailedException(BackendReason.SQL,e);
		}
		for (String s : noncompletedquests){
			if (quest_name.equalsIgnoreCase(s))
				throw new BackendFailedException(BackendReason.ALREADYHAVEQUEST);
		}

		// if not repeatable, check if already completed
		List<String> completedquests;
		try {
			completedquests = getQuests(QuestAvailability.COMPLETED,p);
		} catch (SQLException e) {
			MineQuest.log(Level.SEVERE, "[QuestBackend] Invoked giveQuestToPlayer by " +
					p.getName() + " on quest " + quest_name + " threw exception:");
			MineQuest.log(Level.SEVERE, e.toString());
			throw new BackendFailedException(BackendReason.SQL,e);
		}
		for (String s : completedquests){
			if (quest_name.equalsIgnoreCase(s)){
				if (repeatable)
					regive = true;
				else
					throw new BackendFailedException(BackendReason.UNREPEATABLEQUEST);
			}
		}

		if (!regive)
			MineQuest.sqlstorage.querySQL("Quests/giveQuest", p.getName(), quest_name);
		else
			MineQuest.sqlstorage.querySQL("Quests/reGiveQuest", p.getName(), quest_name);
		QuestAvailableEvent event = new QuestAvailableEvent(quest_name, p);
		Bukkit.getPluginManager().callEvent(event);
	}

	/**
	 * Get quests that the player has
	 * @param type State of the quests (available, accepted, completed)
	 * @param p Player
	 * @return List of quest names
	 * @throws SQLException if the backend had trouble querying.
	 */
	public static List<String> getQuests(QuestAvailability type, Player p) throws SQLException{
		ResultSet currentquests;
		if (type == QuestAvailability.ACCEPTED)
			currentquests = MineQuest.sqlstorage.querySQL(
					"Quests/getPlayerQuestsNotCompleted", p.getName());
		else if (type == QuestAvailability.COMPLETED)
			currentquests = MineQuest.sqlstorage.querySQL(
					"Quests/getPlayerQuestsCompleted", p.getName());
		else
			currentquests = MineQuest.sqlstorage.querySQL(
					"Quests/getPlayerQuestsAvailable", p.getName());
		return MineQuest.sqlstorage.getColumn(currentquests, "Q_ID");
	}

	/**
	 * This checks if a quest file exists; and also if it's repeatable.<br>
	 * <b>This has been marked for resignature when the quest file format changes.</b>
	 * Frontends may have to adjust their method calls.
	 * @param quest_name Quest to check
	 * @return true if repeatable
	 */
	public static boolean isRepeatable(String quest_name){
		QuestDescription d = getQuestDesc(quest_name);
		if (d==null)
			throw new IllegalArgumentException("No such Quest!");
		return d.questRepeatable;
	}

	/**
	 * Retrieve details about a particular quest.
	 * @param quest_name Quest to retrieve
	 * @return Details or <code>null</code> if there was no such quest.
	 */
	public static QuestDescription getQuestDesc(String quest_name){
		return MineQuest.questManager.getQuest(quest_name);
	}

	/**
	 * Accept a quest
	 * @param p Player
	 * @param quest_name Quest Name
	 * @throws BackendFailedException if the player doesn't have the quest
	 */
	public static void acceptQuest(Player p, String quest_name) throws BackendFailedException{
		QuestDescription d = getQuestDesc(quest_name);
		if (d==null)
			throw new BackendFailedException(BackendReason.FILEDISAPPEARED);
		try {
			List<String> nonacceptedquests = getQuests(QuestAvailability.AVAILABLE, p);
			for (String s : nonacceptedquests){
				if (s.equalsIgnoreCase(quest_name)){
					MineQuest.sqlstorage.querySQL("Quests/acceptQuest", p.getName(), quest_name);
					p.sendMessage(ChatUtils.chatify(d.displayaccept));
					return;
				}
			}
			throw new BackendFailedException(BackendReason.NOTHAVEQUEST);
		} catch (SQLException e) {
			throw new BackendFailedException(e);
		}
	}
	
	/**
	 * Decline a quest
	 * @param p Player
	 * @param quest_name Quest Name
	 * @throws BackendFailedException if the player doesn't have the quest or has already accepted it
	 */
	public static void declineQuest(Player p, String quest_name) throws BackendFailedException {
		QuestDescription d = getQuestDesc(quest_name);
		if (d==null)
			throw new BackendFailedException(BackendReason.FILEDISAPPEARED);
		try {
			List<String> nonacceptedquests = getQuests(QuestAvailability.AVAILABLE, p);
			for (String s : nonacceptedquests){
				if (s.equalsIgnoreCase(quest_name)){
					MineQuest.sqlstorage.querySQL("Quests/declineQuest", p.getName(), quest_name);
					p.sendMessage(ChatUtils.chatify("Quest declined!"));
					return;
				}
			}
			throw new BackendFailedException(BackendReason.NOTHAVEQUEST);
		} catch (SQLException e) {
			throw new BackendFailedException(e);
		}
	}

	/**
	 * Reload a Quest
	 * @param name Name of Quest to reload, or NULL for all of them
	 * @throws BackendFailedException If there is no such Quest
	 */
	public static void reloadQuest(String name) throws BackendFailedException {
		QuestManager q = MineQuest.questManager;
		if (name==null)
			q.reloadQuests();
		else{
			try {
				q.reloadQuest(name);
			} catch (IllegalArgumentException e){
				throw new BackendFailedException(e);
			}
		}
	}

	/**
	 * Start a quest
	 * @param p <b>LEADER</b> of the team
	 * @param name Quest Name
	 * @throws BackendFailedException If quest couldn't be started
	 */
	/* FIXME NOW DELEGATED TO GROUP */
	/*	public static void startQuest(Player p, String name) throws BackendFailedException {
		long teamid = MineQuest.
		if (teamid==-1){
			teamid = MineQuest.groupManager.createTeam(p);
		}
		Team t = MineQuest.groupManager.getGroup(teamid);
		Player leader = t.getLeader();
		if (leader!=p)
			throw new BackendFailedException("Only leaders can start quests!");
		try {
			List<String> possiblequests = getQuests(QuestAvailability.ACCEPTED, leader);
			for (String s : possiblequests){
				if (s.equalsIgnoreCase(name)){
					MineQuest.questManager.getQuest(MineQuest.questManager.startQuest(s, t)).enterQuest();
				}
			}
			throw new BackendFailedException("No such *accepted* quest!");
		} catch (SQLException e) {
			throw new BackendFailedException(e);
		}
	}*/

	/**
	 * Cancel the active quest (abandon quest)
	 * @param p <b>LEADER</b> of the team that wants to abandon quest
	 * @throws BackendFailedException
	 */
	/* FIXME NOW DELEGATED TO GROUP */
	/*	public static void cancelActiveQuest(Player p) throws BackendFailedException {
		long teamid = MineQuest.playerManager.getPlayerDetails(p).getTeam();
		if (teamid==-1){
			throw new BackendFailedException("You're not on a team!");
		}
		Team t = MineQuest.groupManager.getGroup(teamid);
		Player leader = t.getLeader();
		if (leader!=p)
			throw new BackendFailedException("Only leaders can cancel the current quest!");
		long questid = MineQuest.playerManager.getPlayerDetails(leader).getQuest();
		if (questid==-1)
			throw new BackendFailedException("Not on a quest!");
		MineQuest.questManager.getQuest(questid).finishQuest(CompleteStatus.CANCELED);
	}*/

}
