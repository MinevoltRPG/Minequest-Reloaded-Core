# Accept given quest
# first %s: Player Name
# second %s: Quest Name
UPDATE mq_quest SET ISCOMPLETED = 1 WHERE P_NAME = '%s' AND Q_ID = '%s'