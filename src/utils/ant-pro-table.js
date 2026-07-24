export function getColumnId(column, index, used = new Set()) {
  const dataIndex = Array.isArray(column?.dataIndex)
    ? column.dataIndex.join(".")
    : column?.dataIndex;
  const base = String(column?.key ?? dataIndex ?? `column-${index}`);
  let id = base;
  let suffix = 1;
  while (used.has(id)) {
    id = `${base}-${suffix++}`;
  }
  used.add(id);
  return id;
}

export function createColumnStates(columns = [], previous = []) {
  const previousMap = new Map(previous.map((item) => [item.id, item]));
  const previousOrder = new Map(previous.map((item, index) => [item.id, index]));
  const used = new Set();
  const states = columns.map((column, index) => {
    const id = getColumnId(column, index, used);
    const old = previousMap.get(id);
    return {
      id,
      column,
      visible: old?.visible ?? column.hidden !== true,
      fixed: old ? old.fixed : column.fixed,
      sourceIndex: index,
    };
  });

  states.sort((a, b) => {
    const aOrder = previousOrder.has(a.id) ? previousOrder.get(a.id) : Number.MAX_SAFE_INTEGER + a.sourceIndex;
    const bOrder = previousOrder.has(b.id) ? previousOrder.get(b.id) : Number.MAX_SAFE_INTEGER + b.sourceIndex;
    return aOrder - bOrder;
  });
  const grouped = groupColumns(states);
  return [...grouped.left, ...grouped.none, ...grouped.right];
}

export function groupColumns(states = []) {
  return {
    left: states.filter((item) => item.fixed === "left"),
    none: states.filter((item) => item.fixed !== "left" && item.fixed !== "right"),
    right: states.filter((item) => item.fixed === "right"),
  };
}

export function setColumnVisible(states, id, visible) {
  return states.map((item) => item.id === id ? { ...item, visible } : item);
}

export function setAllColumnsVisible(states, visible) {
  return states.map((item) => ({ ...item, visible }));
}

export function moveColumn(states, id, targetGroup, targetIndex) {
  const item = states.find((state) => state.id === id);
  if (!item) return states.slice();

  const remaining = states.filter((state) => state.id !== id);
  const grouped = groupColumns(remaining);
  const target = grouped[targetGroup] || grouped.none;
  const nextItem = {
    ...item,
    fixed: targetGroup === "left" ? "left" : targetGroup === "right" ? "right" : undefined,
  };
  target.splice(Math.max(0, Math.min(targetIndex, target.length)), 0, nextItem);
  return [...grouped.left, ...grouped.none, ...grouped.right];
}

export function moveColumnByOffset(states, id, offset) {
  const grouped = groupColumns(states);
  const groupName = Object.keys(grouped).find((name) => grouped[name].some((item) => item.id === id));
  if (!groupName) return states.slice();
  const group = grouped[groupName];
  const currentIndex = group.findIndex((item) => item.id === id);
  return moveColumn(states, id, groupName, currentIndex + offset);
}
