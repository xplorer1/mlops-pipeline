import time
import struct
from datetime import datetime
from pymodbus.client import ModbusTcpClient
import logging

# Device register maps (inlined from JSON files)
PAC_4200_REGISTER_MAP = {
    "0": { "name": "Voltage a-n", "unit": "V", "type": "voltage" },
    "2": { "name": "Voltage b-n", "unit": "V", "type": "voltage" },
    "4": { "name": "Voltage c-n", "unit": "V", "type": "voltage" },
    "6": { "name": "Voltage a-b", "unit": "V", "type": "voltage" },
    "8": { "name": "Voltage b-c", "unit": "V", "type": "voltage" },
    "10": { "name": "Voltage c-a", "unit": "V", "type": "voltage" },
    "12": { "name": "Current a", "unit": "A", "type": "current" },
    "14": { "name": "Current b", "unit": "A", "type": "current" },
    "16": { "name": "Current c", "unit": "A", "type": "current" },
    "18": { "name": "Apparent Power a", "unit": "VA", "type": "power" },
    "20": { "name": "Apparent Power b", "unit": "VA", "type": "power" },
    "22": { "name": "Apparent Power c", "unit": "VA", "type": "power" },
    "24": { "name": "Active Power a", "unit": "W", "type": "power" },
    "26": { "name": "Active Power b", "unit": "W", "type": "power" },
    "28": { "name": "Active Power c", "unit": "W", "type": "power" },
    "30": { "name": "Reactive Power a", "unit": "VAr", "type": "power" },
    "32": { "name": "Reactive Power b", "unit": "VAr", "type": "power" },
    "34": { "name": "Reactive Power c", "unit": "VAr", "type": "power" },
    "36": { "name": "Power Factor a", "unit": "-", "type": "pf" },
    "38": { "name": "Power Factor b", "unit": "-", "type": "pf" },
    "40": { "name": "Power Factor c", "unit": "-", "type": "pf" }
}
PAC_3200_REGISTER_MAP = {
    "0": { "name": "Voltage Va-n", "unit": "V", "type": "float32" },
    "2": { "name": "Voltage Vb-n", "unit": "V", "type": "float32" },
    "4": { "name": "Voltage Vc-n", "unit": "V", "type": "float32" },
    "6": { "name": "Voltage Va-b", "unit": "V", "type": "float32" },
    "8": { "name": "Voltage Vb-c", "unit": "V", "type": "float32" },
    "10": { "name": "Voltage Vc-a", "unit": "V", "type": "float32" },
    "12": { "name": "Current a", "unit": "A", "type": "float32" },
    "14": { "name": "Current b", "unit": "A", "type": "float32" },
    "16": { "name": "Current c", "unit": "A", "type": "float32" },
    "18": { "name": "Apparent Power a", "unit": "VA", "type": "float32" },
    "20": { "name": "Apparent Power b", "unit": "VA", "type": "float32" },
    "22": { "name": "Apparent Power c", "unit": "VA", "type": "float32" },
    "24": { "name": "Active Power a", "unit": "W", "type": "float32" },
    "26": { "name": "Active Power b", "unit": "W", "type": "float32" },
    "28": { "name": "Active Power c", "unit": "W", "type": "float32" },
    "30": { "name": "Reactive Power a", "unit": "VAR", "type": "float32" },
    "32": { "name": "Reactive Power b", "unit": "VAR", "type": "float32" },
    "34": { "name": "Reactive Power c", "unit": "VAR", "type": "float32" },
    "36": { "name": "Power Factor a", "unit": "-", "type": "float32" },
    "38": { "name": "Power Factor b", "unit": "-", "type": "float32" },
    "40": { "name": "Power Factor c", "unit": "-", "type": "float32" },
    "42": { "name": "THD-R Voltage a", "unit": "%", "type": "float32" },
    "44": { "name": "THD-R Voltage b", "unit": "%", "type": "float32" },
    "46": { "name": "THD-R Voltage c", "unit": "%", "type": "float32" },
    "48": { "name": "THD-R Current a", "unit": "%", "type": "float32" },
    "50": { "name": "THD-R Current b", "unit": "%", "type": "float32" },
    "52": { "name": "THD-R Current c", "unit": "%", "type": "float32" },
    "54": { "name": "Frequency", "unit": "Hz", "type": "float32" },
    "56": { "name": "Average Voltage Vph-n", "unit": "V", "type": "float32" },
    "58": { "name": "Average Voltage Vph-ph", "unit": "V", "type": "float32" },
    "60": { "name": "Average Current", "unit": "A", "type": "float32" },
    "62": { "name": "Total Apparent Power", "unit": "VA", "type": "float32" },
    "64": { "name": "Total Active Power", "unit": "W", "type": "float32" },
    "66": { "name": "Total Reactive Power", "unit": "VAR", "type": "float32" },
    "68": { "name": "Total Power Factor", "unit": "-", "type": "float32" },
    "70": { "name": "Amplitude Unbalance - Voltage", "unit": "%", "type": "float32" },
    "72": { "name": "Amplitude Unbalance - Current", "unit": "%", "type": "float32" },
    "74": { "name": "Maximum Voltage Va-n", "unit": "V", "type": "float32" },
    "76": { "name": "Maximum Voltage Vb-n", "unit": "V", "type": "float32" },
    "78": { "name": "Maximum Voltage Vc-n", "unit": "V", "type": "float32" },
    "80": { "name": "Max. Voltage Va-b", "unit": "V", "type": "float32" },
    "82": { "name": "Max. Voltage Vb-c", "unit": "V", "type": "float32" },
    "84": { "name": "Max. Voltage Vc-a", "unit": "V", "type": "float32" },
    "86": { "name": "Maximum Current a", "unit": "A", "type": "float32" },
    "88": { "name": "Maximum Current b", "unit": "A", "type": "float32" },
    "90": { "name": "Maximum Current c", "unit": "A", "type": "float32" },
    "92": { "name": "Maximum Apparent Power a", "unit": "VA", "type": "float32" },
    "94": { "name": "Maximum Apparent Power b", "unit": "VA", "type": "float32" },
    "96": { "name": "Maximum Apparent Power c", "unit": "VA", "type": "float32" },
    "98": { "name": "Maximum Active Power a", "unit": "W", "type": "float32" }
}
ION_REGISTER_MAP = {
    "40150": {"label": "Current_A", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40151": {"label": "Current_B", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40152": {"label": "Current_C", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40153": {"label": "Current_4", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40154": {"label": "Current_5", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40155": {"label": "Current_Avg", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40156": {"label": "Current_Avg_Min", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40157": {"label": "Current_Avg_Max", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40158": {"label": "Current_Avg_Mean", "unit": "A", "type": "UINT16", "scaling": 0.1},
    "40159": {"label": "Frequency", "unit": "Hz", "type": "UINT16", "scaling": 0.1},
    "40160": {"label": "Frequency_Min", "unit": "Hz", "type": "UINT16", "scaling": 0.1},
    "40161": {"label": "Frequency_Max", "unit": "Hz", "type": "UINT16", "scaling": 0.1},
    "40162": {"label": "Frequency_Mean", "unit": "Hz", "type": "UINT16", "scaling": 0.1},
    "40163": {"label": "Voltage_Unbalance", "unit": "%", "type": "UINT16", "scaling": 0.1},
    "40164": {"label": "Current_Unbalance", "unit": "%", "type": "UINT16", "scaling": 0.1},
    "40165": {"label": "Phase_Reverse", "unit": "", "type": "UINT16", "scaling": 0.1},
    "40166": {"label": "Voltage_LN_A", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40168": {"label": "Voltage_LN_B", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40170": {"label": "Voltage_LN_C", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40172": {"label": "Voltage_LN_Avg", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40174": {"label": "Voltage_LN_Avg_Max", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40178": {"label": "Voltage_LL_AB", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40180": {"label": "Voltage_LL_BC", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40182": {"label": "Voltage_LL_CA", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40184": {"label": "Voltage_LL_Avg", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40186": {"label": "Voltage_LL_Avg_Max", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40188": {"label": "Voltage_LL_Avg_Mean", "unit": "V", "type": "UINT32", "scaling": 0.1},
    "40198": {"label": "Active_Power_A", "unit": "kW", "type": "INT32", "scaling": 1.0},
    "40200": {"label": "Active_Power_B", "unit": "kW", "type": "INT32", "scaling": 1.0},
    "40202": {"label": "Active_Power_C", "unit": "kW", "type": "INT32", "scaling": 1.0},
    "40204": {"label": "Active_Power_Total", "unit": "kW", "type": "INT32", "scaling": 1.0},
    "40206": {"label": "Active_Power_Total_Max", "unit": "kW", "type": "INT32", "scaling": 1.0},
    "40208": {"label": "Reactive_Power_A", "unit": "kVAR", "type": "INT32", "scaling": 1.0},
    "40210": {"label": "Reactive_Power_B", "unit": "kVAR", "type": "INT32", "scaling": 1.0},
    "40212": {"label": "Reactive_Power_C", "unit": "kVAR", "type": "INT32", "scaling": 1.0},
    "40214": {"label": "Reactive_Power_Total", "unit": "kVAR", "type": "INT32", "scaling": 1.0},
    "40216": {"label": "Reactive_Power_Total_Max", "unit": "kVAR", "type": "INT32", "scaling": 1.0},
    "40218": {"label": "Apparent_Power_A", "unit": "kVA", "type": "INT32", "scaling": 1.0},
    "40220": {"label": "Apparent_Power_B", "unit": "kVA", "type": "INT32", "scaling": 1.0},
    "40222": {"label": "Apparent_Power_C", "unit": "kVA", "type": "INT32", "scaling": 1.0},
    "40224": {"label": "Apparent_Power_Total", "unit": "kVA", "type": "INT32", "scaling": 1.0},
    "40226": {"label": "Apparent_Power_Total_Max", "unit": "kVA", "type": "INT32", "scaling": 1.0},
    "40230": {"label": "Energy_Active_Delivered", "unit": "kWh", "type": "INT32", "scaling": 1.0},
    "40232": {"label": "Energy_Active_Received", "unit": "kWh", "type": "INT32", "scaling": 1.0},
    "40234": {"label": "Energy_Reactive_Delivered", "unit": "kVARh", "type": "INT32", "scaling": 1.0},
    "40236": {"label": "Energy_Reactive_Received", "unit": "kVARh", "type": "INT32", "scaling": 1.0},
    "40238": {"label": "Energy_Apparent_Total", "unit": "kVAh", "type": "INT32", "scaling": 1.0},
    "40262": {"label": "Power_Factor_A", "unit": "", "type": "INT16", "scaling": 0.01},
    "40263": {"label": "Power_Factor_B", "unit": "", "type": "INT16", "scaling": 0.01},
    "40264": {"label": "Power_Factor_C", "unit": "", "type": "INT16", "scaling": 0.01},
    "40265": {"label": "Power_Factor_Total", "unit": "", "type": "INT16", "scaling": 0.01},
    "40266": {"label": "Voltage_THD_A_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40267": {"label": "Voltage_THD_B_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40268": {"label": "Voltage_THD_C_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40269": {"label": "Current_THD_A_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40270": {"label": "Current_THD_B_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40271": {"label": "Current_THD_C_Max", "unit": "%", "type": "INT16", "scaling": 0.01},
    "40272": {"label": "Current_K_Factor_A", "unit": "", "type": "INT16", "scaling": 0.01},
    "40273": {"label": "Current_K_Factor_B", "unit": "", "type": "INT16", "scaling": 0.01},
    "40274": {"label": "Current_K_Factor_C", "unit": "", "type": "INT16", "scaling": 0.01},
    "40275": {"label": "Current_Crest_Factor_A", "unit": "", "type": "INT16", "scaling": 0.01},
    "40276": {"label": "Current_Crest_Factor_B", "unit": "", "type": "INT16", "scaling": 0.01},
    "40277": {"label": "Current_Crest_Factor_C", "unit": "", "type": "INT16", "scaling": 0.01}
}

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Device configurations
DEVICE_CONFIGS = {
    'PAC4200_Meter_001': {
        'device_id': 'PAC4200_Meter_001',
        'host': 'Cz6fjak42i60t64c.myfritz.net',
        'port': 501,
        'unit_id': 1,
        'device_type': 'Siemens PAC 4200 Meter',
        'register_map': PAC_4200_REGISTER_MAP,
        'register_range': 42
    },
    'ION_Power_Meter_001': {
        'device_id': 'ION_Power_Meter_001',
        'host': 'Cz6fjak42i60t64c.myfritz.net',
        'port': 5102,
        'unit_id': 1,
        'device_type': 'Siemens ION Power Meter',
        'register_map': ION_REGISTER_MAP,
        'register_range': 130
    },
    'PAC3200_Meter_001': {
        'device_id': 'PAC3200_Meter_001',
        'host': 'Cz6fjak42i60t64c.myfritz.net',
        'port': 5002,
        'unit_id': 1,
        'device_type': 'Siemens PAC 3200 Meter',
        'register_map': PAC_3200_REGISTER_MAP,
        'register_range': 100
    }
}

def connect_to_device(host, port):
    client = ModbusTcpClient(host, port=port)
    if client.connect():
        print(f"Successfully connected to {host}:{port}")
        return client
    else:
        print(f"Failed to connect to {host}:{port}")
        return None

def read_registers(client, start_addr, count):
    try:
        result = client.read_holding_registers(address=start_addr, count=count)
        if result.isError():
            print(f"Error reading registers {start_addr}-{start_addr+count-1}: {result}")
            return None
        return result.registers
    except Exception as e:
        print(f"Exception reading registers: {e}")
        return None

def parse_register_value(registers, reg_addr, reg_info, device_type):
    try:
        if device_type.lower().startswith("siemens pac"):
            offset = int(reg_addr)
        else:
            offset = int(reg_addr) - 40001
        dtype = reg_info.get("type", "float32").lower()
        scaling = float(reg_info.get("scaling", 1.0))
        label = reg_info.get("name") or reg_info.get("label") or reg_addr
        if dtype in ("float32", "voltage", "current", "power", "pf"):
            if offset + 1 < len(registers):
                msw = registers[offset]
                lsw = registers[offset + 1]
                if registers[offset] == 65535:
                    value = 0.0
                else:
                    value = struct.unpack('>f', struct.pack('>HH', msw, lsw))[0]
                return label, value
        elif dtype in ("uint16", "int16"):
            if offset < len(registers):
                value = registers[offset] * scaling
                return label, value
        elif dtype in ("int32", "uint32"):
            if offset + 1 < len(registers):
                msw = registers[offset]
                lsw = registers[offset + 1]
                raw = (msw << 16) | lsw
                if dtype == "int32" and raw & 0x80000000:
                    raw -= 0x100000000
                value = raw * scaling
                return label, value
    except Exception as e:
        logger.error(f"Error parsing {label} at {reg_addr}: {e}")
    return label, None

def read_all_parameters(client, config):
    print("\n" + "="*60)
    print(f"Reading from {config['device_type']}")
    print(f"IP Address: {config['host']}")
    print(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    start_addr = 0 if not config['device_type'].lower().startswith("siemens pac") else 1
    registers = read_registers(client, start_addr, config['register_range'])
    if registers is None:
        print("Failed to read registers from device")
        return None
    results = {}
    print(f"\n{'Parameter':<30} {'Value':<15} {'Unit':<10}")
    print("-"*60)
    for reg_addr, reg_info in config['register_map'].items():
        name, value = parse_register_value(registers, reg_addr, reg_info, config['device_type'])
        if value is not None:
            unit = reg_info.get("unit", "")
            results[name] = {"value": value, "unit": unit}
            print(f"{name:<30} {value:<15.4f} {unit:<10}")
    return results

def continuous_monitoring(client, config, interval=5):
    print(f"\nStarting continuous monitoring (interval: {interval}s)")
    print("Press Ctrl+C to stop...\n")
    try:
        while True:
            results = read_all_parameters(client, config)
            if results:
                total_active_power = sum(
                    results.get(k, {}).get("value", 0)
                    for k in results if "Active Power" in k or "kW" in k
                )
                print(f"\nTotal Active Power: {total_active_power:.3f} W")
                print(f"Next reading in {interval} seconds...")
                print("="*60)
            time.sleep(interval)
    except KeyboardInterrupt:
        print("\nMonitoring stopped by user")

def main():
    print("Available devices:")
    for i, key in enumerate(DEVICE_CONFIGS.keys(), 1):
        print(f"  {i}. {key}")
    choice = input("Select device by number: ").strip()
    try:
        idx = int(choice) - 1
        key = list(DEVICE_CONFIGS.keys())[idx]
    except Exception:
        print("Invalid selection.")
        return
    config = DEVICE_CONFIGS[key]
    client = connect_to_device(config['host'], config['port'])
    if not client:
        return
    try:
        print("\nPerforming initial reading...")
        read_all_parameters(client, config)
        continuous_monitoring(client, config, 5)
    except Exception as e:
        print(f"Error during execution: {e}")
    finally:
        client.close()
        print("Connection closed")

if __name__ == "__main__":
    main()