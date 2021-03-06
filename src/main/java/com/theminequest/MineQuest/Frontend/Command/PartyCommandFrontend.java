package com.theminequest.MineQuest.Frontend.Command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.theminequest.MineQuest.I18NMessage;
import com.theminequest.MineQuest.MineQuest;
import com.theminequest.MineQuest.Backend.BackendFailedException;
import com.theminequest.MineQuest.Backend.GroupBackend;
import com.theminequest.MineQuest.Group.Group;
import com.theminequest.MineQuest.Group.GroupException;
import com.theminequest.MineQuest.Group.GroupException.GroupReason;
import com.theminequest.MineQuest.Utils.ChatUtils;

public class PartyCommandFrontend extends CommandFrontend {

	public PartyCommandFrontend() {
		super("party");
	}

	public Boolean accept(Player p, String[] args) {
		if (GroupBackend.teamID(p)!=-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_INPARTY.getDescription());
			return false;
		}
		if (!GroupBackend.hasInvite(p)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_NOINVITE.getDescription());
			return false;
		}
		try {
			GroupBackend.acceptInvite(p);
			p.sendMessage(I18NMessage.Cmd_Party_ACCEPT.getDescription());
			return true;
		} catch (BackendFailedException e) {
			e.printStackTrace();
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			return false;
		}
	}

	public Boolean create(Player p, String[] args) {
		if (GroupBackend.teamID(p)!=-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_INPARTY.getDescription());
			return false;
		}
		if (GroupBackend.hasInvite(p)){
			p.sendMessage(ChatColor.GRAY + I18NMessage.Cmd_Party_DISCARD.getDescription());
		}
		try {
			GroupBackend.createTeam(p);
			p.sendMessage(I18NMessage.Cmd_Party_CREATE.getDescription());
			return true;
		} catch (BackendFailedException e) {
			e.printStackTrace();
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			return false;
		}
	}

	public Boolean invite(Player p, String[] args) {
		if (GroupBackend.teamID(p)==-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOPARTY.getDescription());
			return false;
		}
		if (args.length!=1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_INVALIDARGS.getDescription());
			return false;
		}
		Group g = GroupBackend.getCurrentGroup(p);
		if (!g.getLeader().equals(p)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOTLEADER.getDescription());
			return false;
		}

		Player mate = Bukkit.getPlayerExact(args[0]);
		if (mate==null){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOSUCHPLAYER.getDescription());
			return false;
		}
		if (GroupBackend.teamID(mate)!=-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_TARGETINPARTY.getDescription());
			return false;
		}
		try {
			g.invite(mate);
			p.sendMessage(I18NMessage.Cmd_Party_TARGETINVITESENT.getDescription());
			return true;
		} catch (GroupException e) {
			if (e.getReason()==GroupReason.ALREADYINTEAM){
				p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_TARGETPENDING.getDescription());
				return false;
			}
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public Boolean kick(Player p, String[] args) {
		if (GroupBackend.teamID(p)==-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOPARTY.getDescription());
			return false;
		}
		if (args.length!=1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_INVALIDARGS.getDescription());
			return false;
		}
		Group g = GroupBackend.getCurrentGroup(p);
		if (!g.getLeader().equals(p)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOTLEADER.getDescription());
			return false;
		}
		Player mate = Bukkit.getPlayer(args[0]);
		if (!g.contains(mate)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_TARGETNOPARTY.getDescription());
			return false;
		}

		try {
			g.remove(mate);
			p.sendMessage(I18NMessage.Cmd_Party_KICK + mate.getDisplayName());
			mate.sendMessage(I18NMessage.Cmd_Party_KICKTARGET + " (" + p.getDisplayName() + ")");
			return true;
		} catch (GroupException e) {
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}

	public Boolean leave(Player p, String[] args) {
		if (GroupBackend.teamID(p)==-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOPARTY.getDescription());
			return false;
		}
		if (args.length!=0){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_INVALIDARGS.getDescription());
			return false;
		}
		Group g = GroupBackend.getCurrentGroup(p);
		boolean leader = g.getLeader().equals(p);

		try {
			if (leader && g.getPlayers().size()!=1)
				g.setLeader(g.getPlayers().get(1));
			g.remove(p);
			p.sendMessage(I18NMessage.Cmd_Party_LEAVE.getDescription());
			return true;
		} catch (GroupException e) {
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}
	
	public Boolean list(Player p, String[] args) {
		if (GroupBackend.teamID(p)==-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOPARTY.getDescription());
			return false;
		}
		if (args.length!=0){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_INVALIDARGS.getDescription());
			return false;
		}
		Group g = GroupBackend.getCurrentGroup(p);
		List<Player> members = g.getPlayers();
		List<String> messages = new ArrayList<String>();
		messages.add(ChatUtils.formatHeader(I18NMessage.Cmd_Party_LIST.getDescription() + " " + members.size() + "/" + g.getCapacity()));
		for (Player m : members) {
			if (g.getLeader().equals(m))
				messages.add(ChatColor.RED + m.getName() + ChatColor.GRAY + " : Lvl " + m.getLevel() + ", Health " + m.getHealth() + "/" + m.getMaxHealth());
			else
				messages.add(ChatColor.AQUA + m.getName() + ChatColor.GRAY + " : Lvl " + m.getLevel() + ", Health " + m.getHealth() + "/" + m.getMaxHealth());
		}
		
		for (String m : messages) {
			p.sendMessage(m);
		}
		
		return true;
	}
	
	public Boolean promote(Player p, String[] args) {
		if (GroupBackend.teamID(p)==-1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOPARTY.getDescription());
			return false;
		}
		if (args.length!=1){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_INVALIDARGS.getDescription());
			return false;
		}
		Group g = GroupBackend.getCurrentGroup(p);
		if (!g.getLeader().equals(p)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_NOTLEADER.getDescription());
			return false;
		}
		Player mate = Bukkit.getPlayer(args[0]);
		if (!g.contains(mate)){
			p.sendMessage(ChatColor.RED + I18NMessage.Cmd_Party_TARGETNOPARTY.getDescription());
			return false;
		}
		
		try {
			g.setLeader(mate);
			mate.sendMessage(I18NMessage.Cmd_Party_PROMOTETARGET.getDescription());
			p.sendMessage(I18NMessage.Cmd_Party_PROMOTE.getDescription());
			return true;
		} catch (GroupException e) {
			p.sendMessage(ChatColor.GRAY + "ERR: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Boolean help(Player p, String[] args) {
		/*
		 * accept
		 * create
		 * 
		 * info
		 * invite
		 * kick
		 * leave
		 * list
		 * promote
		 */

		List<String> messages = new ArrayList<String>();
		Group g = null;
		boolean invite = GroupBackend.hasInvite(p);
		boolean isLeader = false;
		if (GroupBackend.teamID(p)!=-1){
			g = GroupBackend.getCurrentGroup(p);
			isLeader = g.getLeader().equals(p);
		}

		messages.add(ChatUtils.formatHeader(I18NMessage.Cmd_Party_HELP.getDescription()));
		if (g==null){
			if (invite)
				messages.add(ChatUtils.formatHelp("party accept", I18NMessage.Cmd_Party_HELPINVITE.getDescription()));
			else
				messages.add(ChatColor.GRAY + I18NMessage.Cmd_Party_NOINVITE.getDescription());
			messages.add(ChatUtils.formatHelp("party create", I18NMessage.Cmd_Party_HELPCREATE.getDescription()));
			messages.add(ChatColor.AQUA + I18NMessage.Cmd_NOPARTY.getDescription());
		} else {
			if (isLeader){
				messages.add(ChatUtils.formatHelp("party invite <name>", I18NMessage.Cmd_Party_HELPINVITETARGET.getDescription()));
				messages.add(ChatUtils.formatHelp("party kick <name>", I18NMessage.Cmd_Party_HELPKICK.getDescription()));
			} else {
				messages.add(ChatColor.GRAY + "[party invite] " + I18NMessage.Cmd_NOTLEADER.getDescription());
				messages.add(ChatColor.GRAY + "[party kick] " + I18NMessage.Cmd_NOTLEADER.getDescription());
			}
			messages.add(ChatUtils.formatHelp("party leave", I18NMessage.Cmd_Party_HELPLEAVE.getDescription()));
			messages.add(ChatUtils.formatHelp("party list", I18NMessage.Cmd_Party_HELPLIST.getDescription()));
			if (isLeader)
				messages.add(ChatUtils.formatHelp("party promote <name>", I18NMessage.Cmd_Party_HELPPROMOTE.getDescription()));
			else
				messages.add(ChatColor.GRAY + "[party promote] " + I18NMessage.Cmd_NOTLEADER.getDescription());
		}

		for (String s : messages)
			p.sendMessage(s);
		return true;
	}

	@Override
	public boolean allowConsole() {
		return false;
	}

}
