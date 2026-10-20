<script setup>
// 空状态组件：列表无数据时展示
defineProps({
  text: { type: String, default: '暂无内容' },
  icon: { type: String, default: '🐾' }
})
</script>

<template>
  <div class="empty-box">
    <div class="empty-icon-ring">
      <div class="empty-icon">{{ icon }}</div>
    </div>
    <div class="empty-text">{{ text }}</div>
  </div>
</template>

<style scoped>
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
}
.empty-icon-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #fff4e8;
  border: 1px dashed #ffd9b8;
  margin-bottom: 16px;
  box-shadow: 0 6px 18px rgba(242, 162, 104, 0.14);
}
.empty-icon {
  font-size: 40px;
  line-height: 1;
  transform: translateY(-1px);
}
.empty-text {
  font-size: 14px;
  color: #a49a8f;
  text-align: center;
  line-height: 1.6;
}
</style>
