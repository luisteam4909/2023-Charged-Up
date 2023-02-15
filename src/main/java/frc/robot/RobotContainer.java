// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.PS4Controller.Button;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.intake.IntakeDeploy;
import frc.robot.subsystems.intake.IntakeSubsystem;

public class RobotContainer {
  
  private final CommandXboxController m_driverController = new CommandXboxController(0);
  private final CommandXboxController m_operatorController = new CommandXboxController(1);


  public RobotContainer() {
    configureBindings();
  }

  private void configureBindings() {
    m_driverController.leftTrigger()
      .onTrue(new RunCommand(()-> IntakeSubsystem.getInstance().cubeIn(), IntakeSubsystem.getInstance()))
      .onFalse(new RunCommand(()-> IntakeSubsystem.getInstance().intakeIn(), IntakeSubsystem.getInstance()));
    
    m_driverController.leftBumper()
      .onTrue(new RunCommand(()-> IntakeSubsystem.getInstance().cubeSpit(), IntakeSubsystem.getInstance()))
      .onFalse(new RunCommand(()-> IntakeSubsystem.getInstance().intakeIn(), IntakeSubsystem.getInstance()));
    
    m_driverController.rightTrigger()
      .onTrue(new RunCommand(()-> IntakeSubsystem.getInstance().coneIn(), IntakeSubsystem.getInstance()))
      .onFalse(new RunCommand(()-> IntakeSubsystem.getInstance().intakeIn(), IntakeSubsystem.getInstance()));
    
    m_driverController.rightBumper()
      .onTrue(new RunCommand(()-> IntakeSubsystem.getInstance().coneSpit(), IntakeSubsystem.getInstance()))
      .onFalse(new RunCommand(()-> IntakeSubsystem.getInstance().intakeIn(), IntakeSubsystem.getInstance()));
  }

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }

  private double deadband(double value, double deadband) {
    if (Math.abs(value) > deadband) {
      if (value > 0d)
        return (value - deadband) / (1d - deadband);
      else
        return (value + deadband) / (1d - deadband);
    } else {
      return 0d;
    }
  }

  private double modifyAxis(double value) {
    value = deadband(value, 0.05);
    // Square the axis
    value = Math.copySign(value * value, value);
    return value;
  }
}
