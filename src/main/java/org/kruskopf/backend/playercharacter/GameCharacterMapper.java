package org.kruskopf.backend.playercharacter;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.playercharacter.dto.CharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class GameCharacterMapper {

    public CharacterOutputDTO toOutputDTO(GameCharacter character) {
        return new CharacterOutputDTO(
                character.getId(),
                character.getOwner().getId(),
                character.getCampaign() != null ? character.getCampaign().getId() : null,
                character.getName(),
                character.getSystemType(),
                character.getNotes(),
                character.getImageUrl(),
                toDnd5eOutputDTO(character),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }

    private Dnd5eCharacterDataOutputDTO toDnd5eOutputDTO(GameCharacter character) {
        if (character.getSystemType() != GameSystem.DND5E) {
            return null;
        }
        Dnd5eCharacterData data = character.getDnd5eData();
        if (data == null) {
            return null;
        }
        return new Dnd5eCharacterDataOutputDTO(
                data.getLevel(),
                data.getCharacterClass(),
                data.getRace(),
                data.getHitPoints(),
                data.getArmorClass(),
                data.getStats()
        );
    }

    public GameCharacter toEntity(CharacterInputDTO dto, User owner, Campaign campaign) {
        GameSystem system = dto.systemType() != null ? dto.systemType() : GameSystem.DND5E;

        GameCharacter character = new GameCharacter(owner, campaign, dto.name(), system);
        character.setNotes(dto.notes());
        if (dto.avatarUrl() != null) {
            character.setImageUrl(dto.avatarUrl());
        }

        if (system == GameSystem.DND5E) {
            character.attachDnd5eData(toDnd5eEntity(dto.dnd5e()));
        }

        return character;
    }

    private Dnd5eCharacterData toDnd5eEntity(Dnd5eCharacterDataInputDTO dto) {
        Dnd5eCharacterData data = new Dnd5eCharacterData();
        if (dto != null) {
            if (dto.level() != null) data.setLevel(dto.level());
            if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
            if (dto.race() != null) data.setRace(dto.race());
            if (dto.hitPoints() != null) data.setHitPoints(dto.hitPoints());
            if (dto.armorClass() != null) data.setArmorClass(dto.armorClass());
            if (dto.stats() != null) data.setStats(dto.stats());
        }
        return data;
    }

    public void patchEntity(GameCharacter entity, CharacterInputDTO dto) {
        if (dto.name() != null) entity.setName(dto.name());
        if (dto.systemType() != null) entity.setSystemType(dto.systemType());
        if (dto.notes() != null) entity.setNotes(dto.notes());
        // this is to be able to update the imageUrl
        if (dto.avatarUrl() != null) entity.setImageUrl(dto.avatarUrl());

        // Patch system-specific data only when the active system is D&D 5e.
        if (dto.dnd5e() != null && entity.getSystemType() == GameSystem.DND5E) {
            Dnd5eCharacterData data = entity.getDnd5eData();
            if (data == null) {
                entity.attachDnd5eData(toDnd5eEntity(dto.dnd5e()));
            } else {
                patchDnd5e(data, dto.dnd5e());
            }
        }
    }

    private void patchDnd5e(Dnd5eCharacterData data, Dnd5eCharacterDataInputDTO dto) {
        if (dto.level() != null) data.setLevel(dto.level());
        if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
        if (dto.race() != null) data.setRace(dto.race());
        if (dto.hitPoints() != null) data.setHitPoints(dto.hitPoints());
        if (dto.armorClass() != null) data.setArmorClass(dto.armorClass());
        if (dto.stats() != null) data.setStats(dto.stats());
    }
}
