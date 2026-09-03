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
    public Player self;/**
    * Envia HTML para o jogador.
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
            back();
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
            back();
            return;
        }

        self.stopAutoFarm();

        self.sendMessage("Auto Farm desativado.");

        back();
    }

    /**
     * Alterna Auto Farm.
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
     * Abre Skill 1.
     */
    public void skill1()
    {
        showSkillList(1, 0);
    }

    /**
     * Abre Skill 2.
     */
    public void skill2()
    {
        showSkillList(2, 0);
    }

    /**
     * Abre Skill 3.
     */
    public void skill3()
    {
        showSkillList(3, 0);
    }

    /**
     * Próxima página Skill 1.
     */
    public void skill1next()
    {
        showSkillList(1, 1);
    }

    /**
     * Próxima página Skill 2.
     */
    public void skill2next()
    {
        showSkillList(2, 1);
    }

    /**
     * Próxima página Skill 3.
     */
    public void skill3next()
    {
        showSkillList(3, 1);
    }

    /**
     * Página anterior Skill 1.
     */
    public void skill1prev()
    {
        showSkillList(1, 0);
    }

    /**
     * Página anterior Skill 2.
     */
    public void skill2prev()
    {
        showSkillList(2, 0);
    }

    /**
     * Página anterior Skill 3.
     */
    public void skill3prev()
    {
        showSkillList(3, 0);
    }

    /**
     * Abre menu de seleção de poção (exibe apenas poções presentes no inventário do jogador).
     */
    public void potion()
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();
        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Poção</title>");
        html.append("<body>");
        html.append("<center>");

        html.append("<br>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">SELECIONAR POCAO</font>");
        html.append("<br><br>");

        html.append("<table width=300 border=0 cellpadding=2 cellspacing=3>");

        // Verifica inventário para Greater Healing Potion (ID: 1539)
        if (self.getInventory().getItemByItemId(1539) != null)
        {
            html.append("<tr><td align=center><button value=\"Greater Healing Potion\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setPotion 1539\" width=285 height=27 back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\"></td></tr>");
        }

        // Verifica inventário para Greater Mana Potion (ID: 728)
        if (self.getInventory().getItemByItemId(728) != null)
        {
            html.append("<tr><td align=center><button value=\"Greater Mana Potion\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setPotion 728\" width=285 height=27 back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\"></td></tr>");
        }

        // Verifica inventário para Haste Potion (ID: 735)
        if (self.getInventory().getItemByItemId(735) != null)
        {
            html.append("<tr><td align=center><button value=\"Haste Potion\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setPotion 735\" width=285 height=27 back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\"></td></tr>");
        }

        // Opção padrão para remover poção
        html.append("<tr><td align=center><button value=\"REMOVER POCAO\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setPotion 0\" width=285 height=27 back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\"></td></tr>");
        html.append("</table><br>");

        html.append("<button value=\"VOLTAR\" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\" width=100 height=27 back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");
        html.append("</center></body></html>");

        showHtml(html.toString());
    }

    /**
     * Define a poção selecionada utilizando as variáveis nativas do Player.
     */
    public void setPotion(String[] args)
    {
        if (self == null || args == null || args.length < 1)
            return;

        try
        {
            int itemId = Integer.parseInt(args[0]);
            if (itemId > 0 && self.getInventory().getItemByItemId(itemId) == null)
            {
                self.sendMessage("Você não possui esta poção no inventário.");
                return;
            }
            self.setVar("autofarm_potion", String.valueOf(itemId), -1);
            self.sendMessage("Poção do Auto Farm atualizada.");
            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage("ID de poção inválido.");
        }
    }

    /**
     * Mostra a lista de skills.
     *
     * A lista é exibida em DUAS COLUNAS.
     *
     * 50 skills por página.
     */
    private void showSkillList(int slot, int page)
    {
        if (self == null)
            return;

        StringBuilder html = new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Skills</title>");
        html.append("<body>");

        html.append("<center>");

        /*
        * TÍTULO
        */
        html.append("<br>");
        html.append("<font name=\"hs12\" color=\"LEVEL\">");
        html.append("ESCOLHER SKILL ");
        html.append(slot);
        html.append("</font>");

        html.append("<br>");

        html.append("<font color=\"AAAAAA\">");
        html.append("Clique na skill desejada");
        html.append("</font>");

        html.append("<br><br>");

        /*
        * SEM SKILL
        */
        html.append("<button value=\"SEM SKILL\"");
        html.append(" action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:select ");
        html.append(slot);
        html.append(" 0\"");
        html.append(" width=180 height=28");
        html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

        html.append("<br><br>");

        /*
        * BUSCA SKILLS
        */
        List<Skill> skills =
            new ArrayList<Skill>(self.getAllSkills());

        /*
        * ORDENA ALFABETICAMENTE
        */
        Collections.sort(
            skills,
            new Comparator<Skill>()
            {
                @Override
                public int compare(Skill a, Skill b)
                {
                    if (a == null && b == null)
                        return 0;

                    if (a == null)
                        return 1;

                    if (b == null)
                        return -1;

                    String nameA = a.getName();
                    String nameB = b.getName();

                    if (nameA == null)
                        nameA = "";

                    if (nameB == null)
                        nameB = "";

                    return nameA.compareToIgnoreCase(nameB);
                }
            }
        );

        /*
        * FILTRA SKILLS ATIVAS
        */
        List<Skill> validSkills =
            new ArrayList<Skill>();

        for (Skill skill : skills)
        {
            if (skill == null)
                continue;

            if (!skill.isActive())
                continue;

            if (skill.getName() == null ||
                skill.getName().isEmpty())
                continue;

            validSkills.add(skill);
        }

        /*
        * =====================================================
        * PAGINAÇÃO
        * =====================================================
        *
        * 50 skills por página.
        *
        * 25 linhas.
        *
        * 2 skills por linha.
        */
        final int pageSize = 50;

        int start = page * pageSize;

        int end =
            Math.min(
                start + pageSize,
                validSkills.size()
            );

        /*
        * =====================================================
        * DUAS COLUNAS
        * =====================================================
        */
        html.append(
            "<table width=600 border=0 cellpadding=1 cellspacing=2>"
        );

        int column = 0;

        for (int i = start; i < end; i++)
        {
            Skill skill =
                validSkills.get(i);

            if (column == 0)
                html.append("<tr>");

            html.append("<td width=300 align=center>");

            String name =
                skill.getName();

            /*
            * Evita botão gigante.
            */
            if (name.length() > 22)
                name =
                    name.substring(0, 22);

            html.append("<button value=\"");
            html.append(name);
            html.append("\"");

            html.append(
                " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:select "
            );

            html.append(slot);
            html.append(" ");
            html.append(skill.getId());

            html.append("\"");

            html.append(" width=285 height=27");
            html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

            html.append("</td>");

            column++;

            /*
            * Segunda coluna.
            */
            if (column == 2)
            {
                html.append("</tr>");
                column = 0;
            }
        }

        /*
        * Fecha linha incompleta.
        */
        if (column != 0)
        {
            html.append("<td width=300></td>");
            html.append("</tr>");
        }

        html.append("</table>");

        /*
        * =====================================================
        * PAGINAÇÃO
        * =====================================================
        */
        html.append("<br>");

        html.append(
            "<table width=600 border=0 cellpadding=0 cellspacing=2>"
        );

        html.append("<tr>");

        /*
        * ANTERIOR
        */
        if (page > 0)
        {
            html.append("<td width=200 align=center>");

            html.append("<button value=\"< ANTERIOR\"");

            html.append(
                " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:"
            );

            if (slot == 1)
                html.append("skill1prev");
            else if (slot == 2)
                html.append("skill2prev");
            else
                html.append("skill3prev");

            html.append("\"");

            html.append(" width=140 height=27");
            html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

            html.append("</td>");
        }
        else
        {
            html.append("<td width=200></td>");
        }

        /*
        * INDICADOR
        */
        html.append("<td width=200 align=center>");

        html.append("<font color=\"AAAAAA\">");
        html.append("Página ");
        html.append(page + 1);

        if (validSkills.size() > pageSize)
        {
            html.append(" / ");
            html.append(
                (int)Math.ceil(
                    (double)validSkills.size() /
                    pageSize
                )
            );
        }

        html.append("</font>");

        html.append("</td>");

        /*
        * PRÓXIMA
        */
        if (end < validSkills.size())
        {
            html.append("<td width=200 align=center>");

            html.append("<button value=\"PRÓXIMA >\"");

            html.append(
                " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:"
            );

            if (slot == 1)
                html.append("skill1next");
            else if (slot == 2)
                html.append("skill2next");
            else
                html.append("skill3next");

            html.append("\"");

            html.append(" width=140 height=27");
            html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

            html.append("</td>");
        }
        else
        {
            html.append("<td width=200></td>");
        }

        html.append("</tr>");
        html.append("</table>");

        html.append("<br>");

        /*
        * VOLTAR
        */
        html.append("<button value=\"VOLTAR\"");

        html.append(
            " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\""
        );

        html.append(" width=100 height=27");
        html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

        html.append("</center>");

        html.append("</body>");
        html.append("</html>");

        showHtml(html.toString());
    }

    /**
     * Recebe a seleção da skill.
     *
     * skillId = 0 -> Sem Skill.
     */
    public void select(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 2)
            return;

        try
        {
            int slot =
                Integer.parseInt(args[0]);

            int skillId =
                Integer.parseInt(args[1]);

            if (slot < 1 || slot > 3)
                return;

            /*
            * =================================================
            * SEM SKILL
            * =================================================
            */
            if (skillId == 0)
            {
                int skill1 =
                    self.getAutoFarmSkill1();

                int skill2 =
                    self.getAutoFarmSkill2();

                int skill3 =
                    self.getAutoFarmSkill3();

                if (slot == 1)
                    skill1 = 0;
                else if (slot == 2)
                    skill2 = 0;
                else
                    skill3 = 0;

                self.setAutoFarmSkills(
                    skill1,
                    skill2,
                    skill3
                );

                self.sendMessage(
                    "Skill " + slot + " removida."
                );

                back();

                return;
            }

            /*
            * =================================================
            * VERIFICA SKILL
            * =================================================
            */
            Skill skill =
                self.getKnownSkill(skillId);

            if (skill == null)
            {
                self.sendMessage(
                    "Essa skill não pertence ao seu personagem."
                );

                return;
            }

            // Validação ajustada: a skill só pode ser selecionada se o personagem possuir mana suficiente para conjurá-la.
            if (self.getCurrentMp() < skill.getMpConsume())
            {
                self.sendMessage("Você não tem mana suficiente para selecionar esta skill.");
                return;
            }

            /*
            * =================================================
            * MANTÉM OS OUTROS SLOTS
            * =================================================
            */
            int skill1 =
                self.getAutoFarmSkill1();

            int skill2 =
                self.getAutoFarmSkill2();

            int skill3 =
                self.getAutoFarmSkill3();

            if (slot == 1)
                skill1 = skillId;
            else if (slot == 2)
                skill2 = skillId;
            else
                skill3 = skillId;

            self.setAutoFarmSkills(
                skill1,
                skill2,
                skill3
            );

            self.sendMessage(
                "Skill " +
                slot +
                " definida: " +
                skill.getName()
            );

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage(
                "Skill inválida."
            );
        }
    }

    /**
     * Abre seleção do raio.
     */
    public void radius()
    {
        if (self == null)
            return;

        StringBuilder html =
            new StringBuilder();

        html.append("<html noscrollbar>");
        html.append("<title>Auto Farm - Raio</title>");
        html.append("<body>");

        html.append("<center>");

        html.append("<br>");
        html.append(
            "<font name=\"hs12\" color=\"LEVEL\">"
        );

        html.append("SELECIONE O RAIO");

        html.append("</font>");

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
            html.append("<button value=\"");
            html.append(radius);
            html.append("\"");

            html.append(
                " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:setRadius "
            );

            html.append(radius);

            html.append("\"");

            html.append(
                " width=150 height=27"
            );
            html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

            html.append("<br>");
        }

        html.append("<br>");

        html.append("<button value=\"VOLTAR\"");

        html.append(
            " action=\"bypass _bbsscripts;l2f.gameserver.autofarm.AutoFarmCommunity:back\""
        );

        html.append(
            " width=100 height=27"
        );
        html.append(" back=\"L2UI_CH3.Btn_BF_Down\" fore=\"L2UI_CH3.Btn_BF\">");

        html.append("</center>");

        html.append("</body>");
        html.append("</html>");

        showHtml(html.toString());
    }

    /**
     * Define o raio.
     */
    public void setRadius(String[] args)
    {
        if (self == null)
            return;

        if (args == null || args.length < 1)
            return;

        try
        {
            int radius =
                Integer.parseInt(args[0]);

            if (radius != 500 &&
                radius != 750 &&
                radius != 1000 &&
                radius != 1500 &&
                radius != 2000)
            {
                self.sendMessage(
                    "Raio inválido."
                );

                return;
            }

            self.setAutoFarmRadius(radius);

            self.sendMessage(
                "Raio do Auto Farm definido para " +
                radius +
                "."
            );

            back();
        }
        catch (NumberFormatException e)
        {
            self.sendMessage(
                "Raio inválido."
            );
        }
    }

    /**
     * Volta para o menu principal.
     */
    public void back()
    {
        if (self == null)
            return;

        String html =
            HtmCache.getInstance().getNotNull(
                "scripts/services/communityPVP/pages/AutoFarm.htm",
                self
            );

        html = html.replace(
            "%skill1%",
            getSkillName(
                self.getAutoFarmSkill1()
            )
        );

        html = html.replace(
            "%skill2%",
            getSkillName(
                self.getAutoFarmSkill2()
            )
        );

        html = html.replace(
            "%skill3%",
            getSkillName(
                self.getAutoFarmSkill3()
            )
        );

        html = html.replace(
            "%potion%",
            getPotionName(
                self.getVar("autofarm_potion")
            )
        );

        html = html.replace(
            "%radius%",
            String.valueOf(
                self.getAutoFarmRadius()
            )
        );

        html = html.replace(
            "%status%",
            self.isAutoFarm()
                ? "ATIVADO"
                : "DESATIVADO"
        );

        ShowBoard.separateAndSend(
            html,
            self
        );
    }

    /**
     * Retorna o nome da skill.
     */
    private String getSkillName(int skillId)
    {
        if (skillId <= 0)
            return "Sem Skill";

        if (self == null)
            return "Sem Skill";

        Skill skill =
            self.getKnownSkill(skillId);

        if (skill == null)
            return "Sem Skill";

        return skill.getName();
    }

    /**
     * Retorna o nome da poção com base no ID salvo.
     */
    private String getPotionName(String varVal)
    {
        if (varVal == null || varVal.isEmpty())
            return "Nenhuma";

        try
        {
            int itemId = Integer.parseInt(varVal);
            if (itemId <= 0)
                return "Nenhuma";
            if (itemId == 1539)
                return "Greater Healing";
            if (itemId == 728)
                return "Greater Mana Potion";
            if (itemId == 735)
                return "Haste Potion";
            return "Item ID: " + itemId;
        }
        catch (NumberFormatException e)
        {
            return "Nenhuma";
        }
    }
}
