package com.service;

import com.dto.InputDto;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
public interface IReadConfigSrv {
    void test(InputDto input);

    boolean gatheringConfigs(InputDto input);

    boolean checkConfig(InputDto input);

    boolean pushToGitHub(InputDto input);
}
