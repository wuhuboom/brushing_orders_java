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

const COLUMN_STATE_VERSION = 1;

function groupColumnsByFixed(states = []) {
  return {
    left: states.filter((item) => item.fixed === "left"),
    none: states.filter((item) => item.fixed !== "left" && item.fixed !== "right"),
    right: states.filter((item) => item.fixed === "right"),
  };
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
  const grouped = groupColumnsByFixed(states);
  return [...grouped.left, ...grouped.none, ...grouped.right];
}

export function groupColumns(states = []) {
  const visible = states.filter((item) => item.visible);
  const grouped = groupColumnsByFixed(visible);
  return {
    ...grouped,
    hidden: states.filter((item) => !item.visible),
  };
}

export function createColumnStateConfig(states = []) {
  const items = states
    .filter((item) => typeof item?.id === "string" && item.id)
    .map((item) => ({
      id: item.id,
      visible: item.visible !== false,
      fixed: item.fixed === "left" || item.fixed === "right" ? item.fixed : null,
    }));
  return { version: COLUMN_STATE_VERSION, items };
}

export function serializeColumnStates(states = []) {
  return JSON.stringify(createColumnStateConfig(states));
}

export function deserializeColumnStates(value) {
  if (!value) return [];
  try {
    const parsed = typeof value === "string" ? JSON.parse(value) : value;
    if (parsed?.version !== COLUMN_STATE_VERSION || !Array.isArray(parsed.items)) return [];
    const used = new Set();
    return parsed.items.reduce((items, item) => {
      if (typeof item?.id !== "string" || !item.id || used.has(item.id)) return items;
      used.add(item.id);
      items.push({
        id: item.id,
        visible: item.visible !== false,
        fixed: item.fixed === "left" || item.fixed === "right" ? item.fixed : undefined,
      });
      return items;
    }, []);
  } catch {
    return [];
  }
}

export function mergeColumnStatePreferences(preferences = [], states = []) {
  const merged = states.slice();
  const mergedIds = new Set(merged.map((item) => item.id));
  preferences.forEach((item, preferenceIndex) => {
    if (mergedIds.has(item.id)) return;
    let insertionIndex = -1;
    for (let index = preferenceIndex - 1; index >= 0; index -= 1) {
      const previousIndex = merged.findIndex((column) => column.id === preferences[index].id);
      if (previousIndex >= 0) {
        insertionIndex = previousIndex + 1;
        break;
      }
    }
    if (insertionIndex < 0) {
      for (let index = preferenceIndex + 1; index < preferences.length; index += 1) {
        const nextIndex = merged.findIndex((column) => column.id === preferences[index].id);
        if (nextIndex >= 0) {
          insertionIndex = nextIndex;
          break;
        }
      }
    }
    merged.splice(insertionIndex < 0 ? merged.length : insertionIndex, 0, item);
    mergedIds.add(item.id);
  });
  return merged;
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
  const grouped = groupColumnsByFixed(remaining);
  const target = grouped[targetGroup] || grouped.none;
  const nextItem = {
    ...item,
    fixed: targetGroup === "left" ? "left" : targetGroup === "right" ? "right" : undefined,
  };
  const visibleIndexes = target.reduce((indexes, column, index) => {
    if (column.visible) indexes.push(index);
    return indexes;
  }, []);
  const normalizedIndex = Math.max(0, Math.min(targetIndex, visibleIndexes.length));
  const insertionIndex = normalizedIndex < visibleIndexes.length
    ? visibleIndexes[normalizedIndex]
    : target.length;
  target.splice(insertionIndex, 0, nextItem);
  return [...grouped.left, ...grouped.none, ...grouped.right];
}

export function moveColumnByOffset(states, id, offset) {
  const grouped = groupColumns(states);
  const groupName = ["left", "none", "right"]
    .find((name) => grouped[name].some((item) => item.id === id));
  if (!groupName) return states.slice();
  const group = grouped[groupName];
  const currentIndex = group.findIndex((item) => item.id === id);
  return moveColumn(states, id, groupName, currentIndex + offset);
}
