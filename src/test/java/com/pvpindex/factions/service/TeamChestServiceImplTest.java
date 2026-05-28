package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.TeamChestModel;
import com.pvpindex.factions.data.repository.TeamChestRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamChestServiceImpl")
class TeamChestServiceImplTest {

    private Repositories repos;
    private TeamChestRepository chestRepo;
    private FactionsConfig cfg;
    private TeamChestServiceImpl svc;
    private List<TeamChestModel> backing;

    @BeforeEach
    void setUp() throws Exception {
        repos = mock(Repositories.class);
        chestRepo = mock(TeamChestRepository.class);
        cfg = mock(FactionsConfig.class);
        when(repos.teamChests()).thenReturn(chestRepo);
        when(cfg.getMaxTeamChests()).thenReturn(1);
        backing = new ArrayList<>();
        when(chestRepo.findByFactionId("f1")).thenAnswer(inv -> new ArrayList<>(backing));
        when(chestRepo.findByFactionIdAndName("f1", "default")).thenAnswer(inv ->
            backing.stream().filter(c -> c.getName().equalsIgnoreCase("default")).findFirst());
        when(chestRepo.findByFactionIdAndName("f1", "x")).thenAnswer(inv ->
            backing.stream().filter(c -> c.getName().equalsIgnoreCase("x")).findFirst());
        svc = new TeamChestServiceImpl(repos, cfg, Logger.getLogger("test"));
    }

    @Test
    void createRespectsLimit() throws Exception {
        doAnswer(inv -> {
            TeamChestModel m = inv.getArgument(0);
            backing.removeIf(x -> x.getId().equals(m.getId()));
            backing.add(m);
            return null;
        }).when(chestRepo).save(org.mockito.ArgumentMatchers.any());
        assertTrue(svc.createChest("f1", "default"));
        assertFalse(svc.createChest("f1", "x"));
    }

    @Test
    void ensureAutoCreates() throws Exception {
        doAnswer(inv -> {
            TeamChestModel m = inv.getArgument(0);
            backing.add(m);
            return null;
        }).when(chestRepo).save(org.mockito.ArgumentMatchers.any());
        assertEquals(Optional.of("default"), svc.ensureChestExistsForOpen("f1", "default"));
        assertEquals(1, backing.size());
    }

    @Test
    void deleteMissingReturnsFalse() throws StorageException {
        assertFalse(svc.deleteChest("f1", "default"));
    }

    @Test
    void setAndGetContentsRoundTrip() throws Exception {
        final TeamChestModel model = new TeamChestModel(UUID.randomUUID().toString());
        model.setFactionId("f1");
        model.setName("default");
        model.setContents(TeamChestSerialization.encode(List.of()));
        backing.add(model);
        doAnswer(inv -> null).when(chestRepo).save(org.mockito.ArgumentMatchers.any());
        assertTrue(svc.setChestContents("f1", "default", Arrays.asList((org.bukkit.inventory.ItemStack) null)));
        assertTrue(svc.getChestContents("f1", "default").isPresent());
        assertEquals(1, svc.getChestContents("f1", "default").orElse(List.of()).size());
    }
}
