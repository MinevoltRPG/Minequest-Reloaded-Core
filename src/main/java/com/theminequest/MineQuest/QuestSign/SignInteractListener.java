package com.theminequest.MineQuest.QuestSign;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.theminequest.MineQuest.MineQuest;

public class SignInteractListener implements Listener {
	public static void onPlayerInteract(PlayerInteractEvent event){
		Action action = event.getAction();
		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK){
			return;
		}
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		
		if (QuestSign.signCheck(block) == true){
			Sign sign = (Sign) block.getState();
			if (QuestSign.isQuestSign(sign) == true){
				String questName = QuestSign.questName(sign);
				try {
					if (checkQuest(questName) == true){
						//TODO: Add to quest list.
					}
				} catch (FileNotFoundException e) {
					//TODO: Log Error (Quest Not Found)
					e.printStackTrace();
				} 
			}
		}
	}
	public static boolean checkQuest(String questName) throws FileNotFoundException{
		File f = new File(MineQuest.activePlugin.getDataFolder()+File.separator+"quests"+File.separator+questName+".quest");

		if (f.exists() != true){
			return true;
		}
		else{
			return false;
		}
	}
}