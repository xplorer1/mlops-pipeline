import time
import struct
from datetime import datetime
from pymodbus.client import ModbusTcpClient
import logging

# PAC 4200 register map (inlined from JSON file)
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

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# PAC 4200 configuration
PAC4200_CONFIG = {
    'device_id': 'PAC4200_Meter_001',
    'host': 'Cz6fjak42i60t64c.myfritz.net',
    'port': 501,
    'unit_id': 1,
    'device_type': 'Siemens PAC 4200 Meter',
    'register_map': PAC_4200_REGISTER_MAP,
    'register_range': 42
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
    start_addr = 1  # PAC 4200 starts at 1
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
    config = PAC4200_CONFIG
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