@echo off
setlocal enabledelayedexpansion
chcp 65001
cls

REM --- Configuration ---
set "JAVAFX_SDK_VERSION=17.0.15"
set "JAVAFX_SDK_DOWNLOAD_URL=https://download2.gluonhq.com/openjfx/%JAVAFX_SDK_VERSION%/openjfx-%JAVAFX_SDK_VERSION%_windows-x64_bin-sdk.zip"
set "JAVAFX_SDK_ZIP_NAME=javafx-sdk-%JAVAFX_SDK_VERSION%.zip"
set "JAVAFX_SDK_EXTRACTED_FOLDER_NAME=javafx-sdk-%JAVAFX_SDK_VERSION%"

set "FX_VERSION=v0.2.0"
set "TETRIS_JAR_DOWNLOAD_URL=https://github.com/verazza/fx/releases/download/%FX_VERSION%/fx-verazza-%FX_VERSION%-SNAPSHOT.jar"
set "APP_JAR_NAME=fx-verazza-%FX_VERSION%-SNAPSHOT.jar"

REM set "WORK_DIR_BASE=%USERPROFILE%\Desktop"
set "WORK_DIR_BASE=%~dp0"
set "WORK_DIR_NAME=verazza_tetris_game_env_%FX_VERSION%"
REM set "WORK_DIR=%WORK_DIR_BASE%\%WORK_DIR_NAME%"
REM set base-directory is count-directory
set "WORK_DIR=%~dp0%WORK_DIR_NAME%"

set "JAVAFX_MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.media"
set "DOWNLOAD_TIMEOUT_SECONDS_PS=180"
set "DOWNLOAD_TIMEOUT_SECONDS_WGET=180"
set "WGET_PACKAGE_ID=GnuWin32.Wget"

if /I "%DEBUG_OPTION%"=="true" (
    echo --- Tetris Auto-Setup Batch ---
    echo.
    echo Initial Values Check:
    echo     TETRIS_JAR_URL: [%TETRIS_JAR_DOWNLOAD_URL%]
    echo     APP_JAR: [%APP_JAR_NAME%]
    echo     JAVAFX_SDK_URL: [%JAVAFX_SDK_DOWNLOAD_URL%]
    echo     JAVAFX_ZIP: [%JAVAFX_SDK_ZIP_NAME%]
    echo     WORK_DIR: [%WORK_DIR%]
    echo -------------------------
    echo Press any key to start Step 1 - Working Directory Setup...
    pause
)
cls

if /I "%DEBUG_OPTION%"=="true" (
    echo STEP_1_A_Preparing_working_directory
    pause
    echo DEBUG_POINT_1_BEFORE_BASE_DIR_CHECK
    pause
)
if not exist "%WORK_DIR_BASE%" (
    echo    ERROR_Base_dir_missing [%WORK_DIR_BASE%]
    goto :error_exit
)
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_2_AFTER_BASE_DIR_CHECK
    pause
)

if exist "%WORK_DIR%" (
    if /I "%DEBUG_OPTION%"=="true" echo    Directory_already_exists [%WORK_DIR%]
    goto :skip_mkdir
)

if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_3_BEFORE_MKDIR
    pause
    echo    Creating_directory [%WORK_DIR%]
)
mkdir "%WORK_DIR%"
if errorlevel 1 (
    echo    ERROR_mkdir_failed [%WORK_DIR%]
    goto :error_exit
)
if /I "%DEBUG_OPTION%"=="true" (
    echo    Directory_created.
    pause
)

:skip_mkdir
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_4_AFTER_MKDIR_LOGIC
    pause
)

cd /D "%WORK_DIR%"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_5_AFTER_CD Current_directory_is [%CD%]
    pause
)

set "CD_CHECK=%CD%"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_6_CD_CHECK_is [%CD_CHECK%]
    pause
)

if "%CD_CHECK:~-1%"=="\" set "CD_CHECK=%CD_CHECK:~0,-1%"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_7_CD_CHECK_after_slash_removal_is [%CD_CHECK%]
    pause
)

set "WD_CHECK=%WORK_DIR%"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_8_WD_CHECK_is [%WD_CHECK%]
    pause
)

if "%WD_CHECK:~-1%"=="\" set "WD_CHECK=%WD_CHECK:~0,-1%"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_9_WD_CHECK_after_slash_removal_is [%WD_CHECK%]
    pause
    echo DEBUG_POINT_10_PRE_IF_COMPARE Comparing [%CD_CHECK%] and [%WD_CHECK%]
    pause
)

set "PATHS_MATCH=FALSE"
if /I "%CD_CHECK%" == "%WD_CHECK%" set "PATHS_MATCH=TRUE"

if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_11_PATHS_MATCH_is [!PATHS_MATCH!]
    pause
)

if "!PATHS_MATCH!" == "FALSE" goto :handle_path_mismatch_step1_v28
goto :path_match_ok_step1_v28

:handle_path_mismatch_step1_v28
echo    ERROR_Directory_mismatch_detected
echo    Current_CD_CHECK: [!CD_CHECK!]
echo    Target_WD_CHECK: [!WD_CHECK!]
goto :error_exit

:path_match_ok_step1_v28
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_POINT_12_Directory_check_passed
    pause
    echo    Current_working_directory_final: [%CD%]
    echo STEP_1_FINISHED
    echo.
    echo Press any key for STEP 2 - Download Tetris JAR...
    pause
)
cls

REM --- Download Tetris JAR (Inline) ---
if /I "%DEBUG_OPTION%"=="true" echo STEP_2_of_6_Acquiring_Tetris_JAR_Filename_is [%APP_JAR_NAME%]
set "CURRENT_DL_URL=%TETRIS_JAR_DOWNLOAD_URL%"
set "CURRENT_OUT_FILE=%APP_JAR_NAME%"
set "CURRENT_FILE_DESC=Tetris_JAR"
set "DOWNLOAD_SUCCESS=0"
if /I "%DEBUG_OPTION%"=="true" (
    echo "Finished setting variables."
    pause
)
if exist "%CURRENT_OUT_FILE%" (
    if /I "%DEBUG_OPTION%"=="true" echo "A"
    if /I "%DEBUG_OPTION%"=="true" echo     %CURRENT_FILE_DESC% already exists. Skipping download.
    set "DOWNLOAD_SUCCESS=1"
) else (
    if /I "%DEBUG_OPTION%"=="true" echo "B"
    if /I "%DEBUG_OPTION%"=="true" echo    Attempting to download %CURRENT_FILE_DESC% from URL: [%CURRENT_DL_URL%]
    where wget >nul 2>nul
    if %errorlevel% equ 0 (
        set "USE_WGET=1"
    ) else (
        set "USE_WGET=0"
        if /I "%DEBUG_OPTION%"=="true" echo     wget not found. Checking winget...
        where winget >nul 2>nul
        if %errorlevel% equ 0 (
            if /I "%DEBUG_OPTION%"=="true" echo     winget found. Installing wget...
            winget install %WGET_PACKAGE_ID% -h --accept-source-agreements --accept-package-agreements --disable-interactivity
            if %errorlevel% equ 0 (
                where wget >nul 2>nul
                if %errorlevel% equ 0 (
                    if /I "%DEBUG_OPTION%"=="true" echo wget now available.
                    set "USE_WGET=1"
                ) else (
                    if /I "%DEBUG_OPTION%"=="true" echo wget still not found after install attempt.
                )
            ) else (
                if /I "%DEBUG_OPTION%"=="true" echo winget install %WGET_PACKAGE_ID% failed.
            )
        ) else (
            if /I "%DEBUG_OPTION%"=="true" echo     winget not found.
        )
    )
    if /I "%DEBUG_OPTION%"=="true" (
        echo "Finished checking winget installation"
        pause
    )
    if "%USE_WGET%"=="1" (
        if /I "%DEBUG_OPTION%"=="true" echo     Using wget for %CURRENT_FILE_DESC%...
        wget -O "%CURRENT_OUT_FILE%" -T %DOWNLOAD_TIMEOUT_SECONDS_WGET% --progress=bar:force -c --no-check-certificate "%CURRENT_DL_URL%"
        if errorlevel 0 (
            if exist "%CURRENT_OUT_FILE%" (
                if /I "%DEBUG_OPTION%"=="true" echo wget download successful.
                set "DOWNLOAD_SUCCESS=1"
            ) else (
                if /I "%DEBUG_OPTION%"=="true" echo wget ok but file not found.
            )
        ) else (
            if /I "%DEBUG_OPTION%"=="true" echo wget download failed. Error: %errorlevel%
            del "%CURRENT_OUT_FILE%" >nul 2>nul
        )
    )
    if "%DOWNLOAD_SUCCESS%"=="0" (
        if /I "%DEBUG_OPTION%"=="true" echo     Falling back to PowerShell for %CURRENT_FILE_DESC% download...
        powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $ProgressPreference='SilentlyContinue'; Write-Host ('PowerShell: Downloading %CURRENT_FILE_DESC% from %CURRENT_DL_URL% ...'); Invoke-WebRequest -Uri '%CURRENT_DL_URL%' -OutFile '%CURRENT_OUT_FILE%' -TimeoutSec %DOWNLOAD_TIMEOUT_SECONDS_PS% -UseBasicParsing; Write-Host ('PowerShell: %CURRENT_FILE_DESC% Download complete.') } catch { Write-Error ('PowerShell Error downloading %CURRENT_FILE_DESC%: ' + $_.Exception.Message); exit 1 }"
        if errorlevel 1 (
            echo PowerShell download failed.
            goto :error_exit_step2_dl
        )
        if not exist "%CURRENT_OUT_FILE%" (
            echo PowerShell success but file missing.
            goto :error_exit_step2_dl
        )
        if /I "%DEBUG_OPTION%"=="true" echo     PowerShell download successful.
        set "DOWNLOAD_SUCCESS=1"
    )
)
if "%DOWNLOAD_SUCCESS%"=="0" (
    echo    ERROR_Failed_to_acquire_Tetris_JAR.
    :error_exit_step2_dl
    goto :error_exit
)
if /I "%DEBUG_OPTION%"=="true" (
    echo STEP_2_FINISHED
    echo.
    echo Press any key for STEP 3 - Download JavaFX SDK...
    pause
)
cls

REM --- Download JavaFX SDK (Inline) ---
if /I "%DEBUG_OPTION%"=="true" (
    echo STEP_3_of_6_Acquiring_JavaFX_SDK_ZIP
    echo    Filename_will_be [%JAVAFX_SDK_ZIP_NAME%]
    pause
)

set "SDK_LIB_PATH_TO_CHECK=%CD%\%JAVAFX_SDK_EXTRACTED_FOLDER_NAME%\lib"
if /I "%DEBUG_OPTION%"=="true" (
    echo DEBUG_STEP3_B_Path_to_check_for_SDK_lib_is [%SDK_LIB_PATH_TO_CHECK%]
    pause
)

if exist "%SDK_LIB_PATH_TO_CHECK%" goto :sdk_already_extracted_v28
goto :sdk_needs_download_v28

:sdk_already_extracted_v28
    if /I "%DEBUG_OPTION%"=="true" (
        echo DEBUG_STEP3_C1_SDK_lib_path_DOES_exist_Skipping_download_and_extraction
        pause
        echo    JavaFX_SDK_already_extracted_or_lib_folder_exists
    )
    goto :sdk_logic_done_v28

:sdk_needs_download_v28
    if /I "%DEBUG_OPTION%"=="true" (
        echo DEBUG_STEP3_C2_SDK_lib_path_does_NOT_exist_Proceeding_with_download
        pause
    )
    set "CURRENT_DL_URL=%JAVAFX_SDK_DOWNLOAD_URL%"
    set "CURRENT_OUT_FILE=%JAVAFX_SDK_ZIP_NAME%"
    set "CURRENT_FILE_DESC=JavaFX_SDK_ZIP"
    if /I "%DEBUG_OPTION%"=="true" (
        echo    DEBUG_STEP3_D_Variables_for_JavaFX_SDK_download
        echo      URL: [%CURRENT_DL_URL%]
        echo      OUT_FILE: [%CURRENT_OUT_FILE%]
        echo      DESC: [%CURRENT_FILE_DESC%]
        pause
    )

    set "DOWNLOAD_SUCCESS=0"
    if /I "%DEBUG_OPTION%"=="true" echo    Checking for existing %CURRENT_FILE_DESC% - Filename: "%CURRENT_OUT_FILE%"...
    if exist "%CURRENT_OUT_FILE%" (
        if /I "%DEBUG_OPTION%"=="true" echo     %CURRENT_FILE_DESC% [ZIP] already exists. Will proceed to extraction.
        set "DOWNLOAD_SUCCESS=1"
    ) else (
        if /I "%DEBUG_OPTION%"=="true" echo    Attempting to download %CURRENT_FILE_DESC% from URL: [%CURRENT_DL_URL%]
        where wget >nul 2>nul
        if %errorlevel% equ 0 (
            set "USE_WGET=1"
        ) else (
            set "USE_WGET=0"
            if /I "%DEBUG_OPTION%"=="true" echo     wget not found. Checking winget...
            where winget >nul 2>nul
            if %errorlevel% equ 0 (
                if /I "%DEBUG_OPTION%"=="true" echo     winget found. Installing wget...
                winget install %WGET_PACKAGE_ID% -h --accept-source-agreements --accept-package-agreements --disable-interactivity
                if %errorlevel% equ 0 (
                    where wget >nul 2>nul
                    if %errorlevel% equ 0 (
                        if /I "%DEBUG_OPTION%"=="true" echo wget now available.
                        set "USE_WGET=1"
                    ) else (
                        if /I "%DEBUG_OPTION%"=="true" echo wget still not found after install attempt.
                    )
                ) else (
                    if /I "%DEBUG_OPTION%"=="true" echo winget install %WGET_PACKAGE_ID% failed.
                )
            ) else (
                if /I "%DEBUG_OPTION%"=="true" echo     winget not found.
            )
        )
        if "%USE_WGET%"=="1" (
            if /I "%DEBUG_OPTION%"=="true" echo     Using wget for %CURRENT_FILE_DESC%...
            wget -O "%CURRENT_OUT_FILE%" -T %DOWNLOAD_TIMEOUT_SECONDS_WGET% --progress=bar:force -c --no-check-certificate "%CURRENT_DL_URL%"
            if errorlevel 0 (
                if exist "%CURRENT_OUT_FILE%" (
                    if /I "%DEBUG_OPTION%"=="true" echo wget download successful.
                    set "DOWNLOAD_SUCCESS=1"
                ) else (
                    if /I "%DEBUG_OPTION%"=="true" echo wget ok but file not found.
                )
            ) else (
                if /I "%DEBUG_OPTION%"=="true" echo wget download failed. Error: %errorlevel%
                del "%CURRENT_OUT_FILE%" >nul 2>nul
            )
        )
        if "%DOWNLOAD_SUCCESS%"=="0" (
            if /I "%DEBUG_OPTION%"=="true" echo     Falling back to PowerShell for %CURRENT_FILE_DESC% download...
            powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $ProgressPreference='SilentlyContinue'; Write-Host ('PowerShell: Downloading %CURRENT_FILE_DESC% from %CURRENT_DL_URL% ...'); Invoke-WebRequest -Uri '%CURRENT_DL_URL%' -OutFile '%CURRENT_OUT_FILE%' -TimeoutSec %DOWNLOAD_TIMEOUT_SECONDS_PS% -UseBasicParsing; Write-Host ('PowerShell: %CURRENT_FILE_DESC% Download complete.') } catch { Write-Error ('PowerShell Error downloading %CURRENT_FILE_DESC%: ' + $_.Exception.Message); exit 1 }"
            if errorlevel 1 (
                echo PowerShell download failed.
                goto :error_exit_step3_dl_v28
            )
            if not exist "%CURRENT_OUT_FILE%" (
                echo PowerShell success but file missing.
                goto :error_exit_step3_dl_v28
            )
            if /I "%DEBUG_OPTION%"=="true" echo     PowerShell download successful.
            set "DOWNLOAD_SUCCESS=1"
        )
    )
    if "%DOWNLOAD_SUCCESS%"=="0" (
        echo    ERROR_Failed_to_acquire_JavaFX_SDK_ZIP.
        :error_exit_step3_dl_v28
        goto :error_exit
    )
    if /I "%DEBUG_OPTION%"=="true" echo.

    REM 4. Extract JavaFX SDK
    if /I "%DEBUG_OPTION%"=="true" echo STEP_4_of_6_Extracting_JavaFX_SDK
    if exist "%JAVAFX_SDK_ZIP_NAME%" (
        powershell -NoProfile -ExecutionPolicy Bypass -Command "try {Write-Host 'PowerShell: Expanding SDK...'; Expand-Archive -Path '%JAVAFX_SDK_ZIP_NAME%' -DestinationPath '.' -Force; Write-Host 'SDK Expanded.'} catch {Write-Error $_.Exception.Message; exit 1}"
        if errorlevel 1 (
            echo    ERROR_Failed_to_extract_JavaFX_SDK_ZIP.
            goto :error_exit
        )
        if /I "%DEBUG_OPTION%"=="true" echo    JavaFX_SDK_extracted_successfully.
    ) else (
        echo    ERROR_JavaFX_SDK_ZIP_file_not_found_for_extraction [%JAVAFX_SDK_ZIP_NAME%].
        goto :error_exit
    )
    goto :sdk_logic_done_v28

:sdk_logic_done_v28
if /I "%DEBUG_OPTION%"=="true" (
    echo STEP_3_and_4_FINISHED.
    echo.
    echo Press any key for STEP 5 - Set JavaFX Path...
    pause
)
cls

REM 5. Set JavaFX SDK Path
if /I "%DEBUG_OPTION%"=="true" echo STEP_5_of_6_Setting_JavaFX_SDK_path
set "PATH_TO_FX_LIB=%CD%\%JAVAFX_SDK_EXTRACTED_FOLDER_NAME%\lib"
if /I "%DEBUG_OPTION%"=="true" echo    JavaFX_Lib_Path_set_to: "%PATH_TO_FX_LIB%"
if not exist "%PATH_TO_FX_LIB%" (
    echo    ERROR_JavaFX_SDK_lib_directory_not_found.
    goto :error_exit
)
if /I "%DEBUG_OPTION%"=="true" (
    echo STEP_5_FINISHED.
    echo.
    echo Press any key for STEP 6 - Run Application...
    pause
)
cls

REM 6. Run Application
if /I "%DEBUG_OPTION%"=="true" echo STEP_6_of_6_Running_Tetris_Application_Filename_is [%WORK_DIR%\%APP_JAR_NAME%]
if not exist "%WORK_DIR%\%APP_JAR_NAME%" (
    echo    ERROR_Application_JAR_file_not_found_at [%WORK_DIR%\%APP_JAR_NAME%].
    goto :error_exit
)
if /I "%DEBUG_OPTION%"=="true" echo.
if /I "%DEBUG_OPTION%"=="true" echo --- Launch Command ---
if /I "%DEBUG_OPTION%"=="true" echo java --module-path "%PATH_TO_FX_LIB%" --add-modules %JAVAFX_MODULES% -jar "%WORK_DIR%\%APP_JAR_NAME%"
if /I "%DEBUG_OPTION%"=="true" echo --- Launching Game ---
if /I "%DEBUG_OPTION%"=="true" (
    echo Press any key to launch...
    pause
)

java --module-path "%PATH_TO_FX_LIB%" --add-modules %JAVAFX_MODULES% -jar "%WORK_DIR%\%APP_JAR_NAME%"

if errorlevel 1 (
    echo.
    echo    ERROR_Application_failed_to_run.
) else (
    echo.
    if /I "%DEBUG_OPTION%"=="true" echo    Application_finished.
)
goto :end_batch

:error_exit
echo.
echo ^>^>^> SCRIPT_ABORTED_DUE_TO_AN_ERROR ^<^<^<
echo Please_review_the_messages_above.
goto :end_batch

:end_batch
if /I "%DEBUG_OPTION%"=="true" echo.
if /I "%DEBUG_OPTION%"=="true" echo Batch_script_finished.
if /I "%DEBUG_OPTION%"=="true" pause
endlocal
