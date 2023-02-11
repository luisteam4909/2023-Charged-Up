package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;

import edu.wpi.first.math.controller.ElevatorFeedforward;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ElevatorConstants;

public class Elevator extends SubsystemBase {

  private static Elevator m_instance = null;
  private ElevatorStates m_state, m_lastState;

  private final TalonFX m_leftExtensionMotor;
  private final TalonFX m_rightExtensionMotor;

  private Timer m_motionProfileTimer;

  public static final TrapezoidProfile.Constraints m_elevatorTrapezoidalConstraints = new TrapezoidProfile.Constraints(
      3000.0 / 60.0, 6000.0 / 60.0);

  private static final ElevatorFeedforward m_elevatorFF = new ElevatorFeedforward(
      0.1, -0.16, 1 / 5880.0);

  public enum ElevatorStates {
    IDLE("Idle"),
    TOP("Top Node Extension"),
    MID_CUBE("Mid Cube"),
    MID_CONE("Mid Cone"),
    RETRACT("Retracted");

    String stateName;

    private ElevatorStates(String name) {
      this.stateName = name;
    }

    public String toString() {
      return this.stateName;
    }
  }

  private Elevator() {
    // TODO change this to CAN FD BUS constant
    m_leftExtensionMotor = new TalonFX(ElevatorConstants.LEFT_MOTOR, "CANivore1");
    m_rightExtensionMotor = new TalonFX(ElevatorConstants.RIGHT_MOTOR, "CANivore1");
    m_rightExtensionMotor.setInverted(true);
    configElevatorMotor(m_leftExtensionMotor);
    configElevatorMotor(m_rightExtensionMotor);
  }

  @Override
  public void periodic() {
    stateMachine();
    SmartDashboard.putNumber("elevatorError", m_leftExtensionMotor.getClosedLoopError());
    SmartDashboard.putNumber("elevatorEncoder", m_leftExtensionMotor.getSelectedSensorPosition());
  }

  private void stateMachine() {
    Command currentElevatorCommand = null;
    if (!m_state.equals(m_lastState)) {
      switch (m_state) {
        case IDLE:
          currentElevatorCommand = Idle();
          break;
        case TOP:
          currentElevatorCommand = SetSetpoint(ElevatorConstants.TOP_SETPOINT);
          break;
        case MID_CUBE:
          currentElevatorCommand = SetSetpoint(ElevatorConstants.MID_CUBE_SETPOINT);
          break;
        case MID_CONE:
          currentElevatorCommand = SetSetpoint(ElevatorConstants.MID_CONE_SETPOINT);
          break;
        case RETRACT:
          currentElevatorCommand = Retract();
        default:
          m_state = ElevatorStates.IDLE;
      }
    }

    m_lastState = m_state;

    if (currentElevatorCommand != null) {
      currentElevatorCommand.schedule();
    }
  }

  private Command Idle() {
    return new InstantCommand(() -> m_leftExtensionMotor.set(TalonFXControlMode.PercentOutput, 0), this);
  }

  private Command Retract() {
    TrapezoidProfile.State targetState, beginLState, beginRState;
    TrapezoidProfile profileL, profileR;
    targetState = new TrapezoidProfile.State(ElevatorConstants.BOTTOM_SETPOINT, 0.0);

    beginLState = new TrapezoidProfile.State(m_leftExtensionMotor.getSelectedSensorPosition(), 0.0);
    beginRState = new TrapezoidProfile.State(m_rightExtensionMotor.getSelectedSensorPosition(), 0.0);

    profileL = new TrapezoidProfile(m_elevatorTrapezoidalConstraints, targetState, beginLState);
    profileR = new TrapezoidProfile(m_elevatorTrapezoidalConstraints, targetState, beginRState);
    m_motionProfileTimer = new Timer();

    m_motionProfileTimer.reset();
    m_motionProfileTimer.start();
    return new RunCommand(() -> {
      double dt = m_motionProfileTimer.get();

      TrapezoidProfile.State currentLState = profileL.calculate(dt);
      double leftFF = m_elevatorFF.calculate(currentLState.velocity);
      TrapezoidProfile.State currentRState = profileR.calculate(dt);
      double rightFF = m_elevatorFF.calculate(currentRState.velocity);

      m_leftExtensionMotor.set(
          TalonFXControlMode.Position, currentLState.position, DemandType.ArbitraryFeedForward, leftFF);
      m_rightExtensionMotor.set(
          TalonFXControlMode.Position, currentRState.position, DemandType.ArbitraryFeedForward, rightFF);
    }, this);
  }

  private Command SetSetpoint(double distance) {
    return new InstantCommand(() -> {
      m_leftExtensionMotor.set(TalonFXControlMode.Position, distance);
      m_rightExtensionMotor.set(TalonFXControlMode.Position, distance);
    }, this);
  }

  private void configElevatorMotor(TalonFX motor) {
    motor.configFactoryDefault();
    motor.setSelectedSensorPosition(0);
    motor.config_kP(0, ElevatorConstants.ELEVATOR_KP);
    motor.config_kD(0, ElevatorConstants.ELEVATOR_KD);
    motor.configClosedLoopPeakOutput(0, ElevatorConstants.PEAK_OUTPUT);
  }

  public void setState(ElevatorStates state) {
    m_state = state;
  }

  public static Elevator getInstance() {
    if (m_instance == null) {
      m_instance = new Elevator();
    }
    return m_instance;
  }
}
