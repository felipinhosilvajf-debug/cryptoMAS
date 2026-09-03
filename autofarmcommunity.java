package l2f.gameserver.autofarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.network.serverpackets.ShowBoard;

public class AutoFarmCommunity
{
    public Player self;

/**
 * Envia uma página HTML para o jogador.
 */
    private void showHtml(String html)
    {
        if (self == null)
            return;

        ShowBoard.separateAndSend(html, self);
    }

    /**
     * Ativa o Auto Farm.
     */
    public void start()
    {
        if (self == null)
            return;

        if (self.isAutoFarm())
        {
            self.sendMessage("Auto Farm já está ativado.");
            return;
        }

        self.startAutoFarm();
        self.sendMessage("Auto Farm ativado.");

        back();
    }

    /**
     * Desativa o Auto Farm.
     */
    public void stop()
    {
        if (self == null)
            return;

        if (!self.isAutoFarm())
        {
            self.sendMessage("Auto Farm já está desativado.");
            return;
        }

        self.stopAutoFarm();
        self.sendMessage("Auto Farm desativado.");

        back();
    }

    /**
     * Alterna entre ativado e desativado.
     */
    public void toggle()
    {
        if (self == null)
            return;

        if (self.isAutoFarm())
            stop();
        else
            start();
    }

    /**
     * Abre seleção da Skill 1.
     */
    public void skill1()
    {
        showSkillList(1);
    }

    /**
     * Abre seleção da Skill 2.
     */
    public void skill2()
    {
        showSkillList(2);
    }

    /**
     * Abre seleção da Skill 3.
     */
    public void skill3()
    {
        showSkillList(3);
    }

    /**
     * Mostra as skills disponíveis do personagem.
     */
    private void showSkillList(int slot)
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Seleção de Skill</title>");
        html.append("<body><br>");

        html.append("<center>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">SELECIONE A SKILL ");
        html.append(slot);
        html.append("</font>");
        html.append("</center>");

        html.append("<br><br>");

        List<Skill> skills = new ArrayList<Skill>(self.getAllSkills());

        Collections.sort(skills, new Comparator<Skill>()
        {
            @Override
            public int compare(Skill a, Skill b)
            {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        int count = 0;

        for (Skill skill : skills)
        {
            if (skill == null)
                continue;

            if (!skill.isActive())
                continue;

            if (skill.getName() == null || skill.getName().isEmpty())
                continue;

            html.append("<table width=600 border=0>");
            html.append("<tr>");

            html.append("<td width=400 align=left>");
            html.append("<font color=\"FFFFFF\">");
            html.append(skill.getName());
            html.append("</font>");
            html.append("</td>");

            html.append("<td width=100 align=center>");
            html.append("<font color=\"AAAAAA\">");
            html.append("Lv.");
            html.append(skill.getLevel());
            html.append("</font>");
            html.append("</td>");

            html.append("<td width=100 align=right>");
            html.append("<button value=\"Usar\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:select ");
            html.append(slot);
            html.append(" ");
            html.append(skill.getId());
            html.append("\" width=80 height=25>");
            html.append("</td>");

            html.append("</tr>");
            html.append("</table>");

            html.append("<br>");

            count++;

            if (count >= 50)
                break;
        }

        if (count == 0)
        {
            html.append("<center>");
            html.append("<font color=\"FF0000\">");
            html.append("Nenhuma skill ativa encontrada.");
            html.append("</font>");
            html.append("</center>");
        }

        html.append("<br>");

        html.append("<center>");
        html.append("<button value=\"VOLTAR\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\" width=100 height=25>");
        html.append("</center>");

        html.append("</body></html>");

        showHtml(html.toString());
    }

    /**
     * Recebe a seleção da skill.
     */
    public void select(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 2)
            return;

        try
        {
            int slot = Integer.parseInt(args[0]);
            int skillId = Integer.parseInt(args[1]);

            Skill skill = self.getKnownSkill(skillId);

            if (skill == null)
            {
                self.sendMessage("Essa skill não pertence ao seu personagem.");
                return;
            }

            int skill1 = self.getAutoFarmSkill1();
            int skill2 = self.getAutoFarmSkill2();
            int skill3 = self.getAutoFarmSkill3();

            if (slot == 1)
                skill1 = skillId;
            else if (slot == 2)
                skill2 = skillId;
            else if (slot == 3)
                skill3 = skillId;
            else
                return;

            self.setAutoFarmSkills(skill1, skill2, skill3);

            self.sendMessage("Skill " + slot + " definida: " + skill.getName());

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage("Skill inválida.");
        }
    }

    /**
     * Abre a tela de seleção do raio.
     */
    public void radius()
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Raio</title>");
        html.append("<body><br>");

        html.append("<center>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">SELECIONE O RAIO</font>");
        html.append("</center>");

        html.append("<br><br>");

        int[] radii =
        {
            500,
            750,
            1000,
            1500,
            2000
        };

        for (int radius : radii)
        {
            html.append("<center>");

            html.append("<button value=\"");
            html.append(radius);
            html.append("\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setRadius ");
            html.append(radius);
            html.append("\" width=150 height=30>");

            html.append("</center>");
            html.append("<br>");
        }

        html.append("<center>");
        html.append("<button value=\"VOLTAR\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\" width=100 height=25>");
        html.append("</center>");

        html.append("</body></html>");

        showHtml(html.toString());
    }

    /**
     * Define o raio do Auto Farm.
     */
    public void setRadius(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 1)
            return;

        try
        {
            int radius = Integer.parseInt(args[0]);

            if (radius != 500 &&
                radius != 750 &&
                radius != 1000 &&
                radius != 1500 &&
                radius != 2000)
            {
                self.sendMessage("Raio inválido.");
                return;
            }

            self.setAutoFarmRadius(radius);

            self.sendMessage("Raio do Auto Farm definido para " + radius + ".");

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage("Raio inválido.");
        }
    }

    /**
     * Volta para o menu principal do Auto Farm.
     */
    public void back()
    {
        if (self == null)
            return;

        String html = HtmCache.getInstance().getNotNull(
            "scripts/services/communityPVP/pages/AutoFarm.htm",
            self);

        html = html.replace(
            "%skill1%",
            getSkillName(self.getAutoFarmSkill1()));

        html = html.replace(
            "%skill2%",
            getSkillName(self.getAutoFarmSkill2()));

        html = html.replace(
            "%skill3%",
            getSkillName(self.getAutoFarmSkill3()));

        html = html.replace(
            "%radius%",
            String.valueOf(self.getAutoFarmRadius()));

        html = html.replace(
            "%status%",
            self.isAutoFarm() ? "ATIVADO" : "DESATIVADO");

        ShowBoard.separateAndSend(html, self);
    }

    /**
     * Retorna o nome da skill pelo ID.
     */
    private String getSkillName(int skillId)
    {
        if (self == null)
            return "Nenhuma";

        Skill skill = self.getKnownSkill(skillId);

        if (skill == null)
            return "Nenhuma";

        return skill.getName();
    }
}
