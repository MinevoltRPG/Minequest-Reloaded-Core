# Accept given quest
# first %s: Player Name
# second %s: Quest Name
UPDATE MQ_QUEST SET ISCOMPLETED = 1 WHERE P_NAME = '%s' AND Q_ID = '%s'